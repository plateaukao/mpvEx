package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

/**
 * Lossless head/tail trim via stream copy (no re-encode).
 *
 * Reads the compressed samples of the source in the kept range and re-muxes them into an MP4
 * container. The start snaps to the nearest keyframe at or before [startMs] (required for the
 * output to be decodable without re-encoding); the end is sample-accurate.
 *
 * Only inputs that Android's [MediaMuxer] can write back into MP4 are supported (H.264/HEVC/AV1
 * video, AAC/… audio). Unsupported inputs fail with [TrimResult.Error] and leave the source
 * untouched — the caller writes the [outputFile] over the original only on success.
 */
object VideoTrimmer {
  sealed interface TrimResult {
    data class Success(
      val outputFile: File,
      /** Actual kept-range start, keyframe-aligned (may be earlier than the requested start). */
      val startOffsetMs: Long,
      /** Duration of the trimmed output. */
      val newDurationMs: Long,
    ) : TrimResult

    data class Error(val message: String) : TrimResult
  }

  /** Fallback sample buffer when the track format doesn't advertise a max input size. */
  private const val FALLBACK_BUFFER_SIZE = 8 * 1024 * 1024

  /**
   * @param outputFile a writable, seekable temp file the muxer writes to.
   * @param onProgress optional 0f..1f progress callback (invoked off the main thread).
   */
  @Suppress("LongMethod", "ReturnCount", "NestedBlockDepth")
  fun trim(
    context: Context,
    sourceUri: Uri,
    startMs: Long,
    endMs: Long,
    outputFile: File,
    onProgress: ((Float) -> Unit)? = null,
  ): TrimResult {
    val startUs = startMs * 1000L
    val endUs = endMs * 1000L
    if (endUs <= startUs) return TrimResult.Error("Invalid trim range")

    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    try {
      // file:// and content:// are both handled by the Context/Uri data source.
      extractor.setDataSource(context, sourceUri, null)

      muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

      // Map extractor track index -> muxer track index for the tracks we keep.
      val indexMap = HashMap<Int, Int>(extractor.trackCount)
      var maxInputSize = 0
      var hasVideo = false
      var rotation = 0

      for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
        if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
        if (mime.startsWith("video/")) {
          hasVideo = true
          rotation = if (format.containsKey(MediaFormat.KEY_ROTATION)) {
            format.getInteger(MediaFormat.KEY_ROTATION)
          } else {
            0
          }
        }
        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
          maxInputSize = maxOf(maxInputSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
        }
        extractor.selectTrack(i)
        // addTrack throws IllegalArgumentException if the muxer can't write this format.
        indexMap[i] = muxer.addTrack(format)
      }

      if (!hasVideo) return TrimResult.Error("No video track to trim")
      if (rotation != 0) muxer.setOrientationHint(rotation)

      val buffer = ByteBuffer.allocate(if (maxInputSize > 0) maxInputSize else FALLBACK_BUFFER_SIZE)
      val bufferInfo = MediaCodec.BufferInfo()

      // Seek to the keyframe at/before the requested start (lossless requirement).
      extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
      val offsetUs = extractor.sampleTime.takeIf { it >= 0 } ?: startUs
      val rangeUs = (endUs - offsetUs).coerceAtLeast(1L)

      muxer.start()
      muxerStarted = true

      var lastPtsUs = 0L
      var wroteSamples = false
      while (true) {
        val sampleTime = extractor.sampleTime
        if (sampleTime < 0 || sampleTime > endUs) break // EOS or past the kept range
        val muxTrack = indexMap[extractor.sampleTrackIndex]
        if (muxTrack == null) {
          if (!extractor.advance()) break
          continue
        }

        buffer.clear()
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) break

        bufferInfo.offset = 0
        bufferInfo.size = size
        bufferInfo.presentationTimeUs = (sampleTime - offsetUs).coerceAtLeast(0L)
        bufferInfo.flags = extractorFlagsToCodecFlags(extractor.sampleFlags)
        muxer.writeSampleData(muxTrack, buffer, bufferInfo)
        lastPtsUs = bufferInfo.presentationTimeUs
        wroteSamples = true

        onProgress?.invoke(((sampleTime - offsetUs).toFloat() / rangeUs).coerceIn(0f, 1f))
        if (!extractor.advance()) break
      }

      if (!wroteSamples) return TrimResult.Error("Nothing to write in the selected range")

      muxer.stop()
      muxerStarted = false
      return TrimResult.Success(
        outputFile = outputFile,
        startOffsetMs = offsetUs / 1000L,
        newDurationMs = lastPtsUs / 1000L,
      )
    } catch (e: Exception) {
      return TrimResult.Error(e.message ?: "Trim failed")
    } finally {
      if (muxerStarted) runCatching { muxer?.stop() }
      runCatching { muxer?.release() }
      runCatching { extractor.release() }
    }
  }

  private fun extractorFlagsToCodecFlags(sampleFlags: Int): Int =
    if (sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
}
