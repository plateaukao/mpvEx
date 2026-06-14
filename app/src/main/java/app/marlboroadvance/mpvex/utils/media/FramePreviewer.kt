package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import `is`.xyz.mpv.FastThumbnails

/**
 * Decodes preview frames at arbitrary positions for the trim scrubbing UI.
 *
 * Primary path is [FastThumbnails] (the app's libmpv-based thumbnailer) with **software decode** —
 * the same fast path the library grid uses. Software decode is important here: while the trim sheet
 * is open the player still holds the hardware video decoder, and a second hardware decode for
 * previews contends for the (few) HW codec instances and stalls for seconds. Software decode of a
 * single keyframe sidesteps that entirely.
 *
 * Falls back to [MediaMetadataRetriever] only when FastThumbnails can't open the source (e.g. a
 * content URI with no real file path). Not thread-safe: build with [create], call [frameAt] from a
 * single background dispatcher, and [release] when done.
 */
class FramePreviewer private constructor(
  private val context: Context,
  private val uri: Uri,
  private val sourcePath: String,
  private val rotationDegrees: Int,
) {
  private var retriever: MediaMetadataRetriever? = null
  private var fastUnavailable = false

  /**
   * Decode the frame at [positionMs]. [retrieverOption] only affects the MediaMetadataRetriever
   * fallback (FastThumbnails already seeks to the nearest keyframe). Returns null on failure.
   */
  fun frameAt(
    positionMs: Long,
    retrieverOption: Int = MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
  ): Bitmap? {
    if (!fastUnavailable) {
      val bmp = runCatching {
        FastThumbnails.generate(sourcePath, positionMs / 1000.0, TARGET_EDGE, false)
      }.getOrNull()
      if (bmp != null) return if (rotationDegrees != 0) rotate(bmp, rotationDegrees) else bmp
      // Don't keep retrying the fast path per-frame once it has proven unable to open this source.
      fastUnavailable = true
    }
    return frameViaRetriever(positionMs, retrieverOption)
  }

  private fun frameViaRetriever(positionMs: Long, option: Int): Bitmap? {
    val r = retriever ?: MediaMetadataRetriever().also { mmr ->
      runCatching {
        if (uri.scheme == "file") mmr.setDataSource(uri.path) else mmr.setDataSource(context, uri)
      }
      retriever = mmr
    }
    return runCatching { r.getFrameAtTime(positionMs * 1000L, option) }.getOrNull()
  }

  fun release() {
    runCatching { retriever?.release() }
    retriever = null
  }

  private fun rotate(src: Bitmap, degrees: Int): Bitmap =
    runCatching {
      Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    }.getOrDefault(src)

  companion object {
    /** Longest edge of the generated preview frame, in pixels. */
    private const val TARGET_EDGE = 512

    /**
     * Build a previewer. [pathHint] should be a real filesystem path when available (used by
     * FastThumbnails); falls back to the URI string otherwise. Suspends briefly to read rotation.
     */
    suspend fun create(
      context: Context,
      uri: Uri,
      pathHint: String,
      displayName: String,
    ): FramePreviewer {
      // FastThumbnails returns frames unrotated, so apply the container rotation ourselves.
      val rotation = runCatching { MediaInfoOps.getRotation(context, uri, displayName) }.getOrDefault(0)
      val source = pathHint.ifBlank { uri.toString() }
      return FramePreviewer(context, uri, source, rotation)
    }
  }
}
