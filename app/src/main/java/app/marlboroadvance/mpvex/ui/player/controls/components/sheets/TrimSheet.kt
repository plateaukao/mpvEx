package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.utils.media.FramePreviewer
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet for lossless head/tail trimming. The user drags a range over the timeline to choose
 * the kept range; a frame preview tracks the handle being dragged so they can see exactly where the
 * cut lands. On confirm the kept range is written back over the original file via [onTrim].
 */
@Composable
fun TrimSheet(
  durationMs: Long,
  previewUri: Uri?,
  previewPath: String?,
  previewName: String,
  onTrim: (startMs: Long, endMs: Long) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val durationSec = (durationMs / 1000L).coerceAtLeast(1L).toFloat()
  var range by remember { mutableStateOf(0f..durationSec) }
  var showConfirm by remember { mutableStateOf(false) }
  // Which handle the preview tracks, and the position (ms) it points at.
  var activeHandleStart by remember { mutableStateOf(true) }
  var previewPosMs by remember { mutableStateOf(0L) }

  val startSec = range.start
  val endSec = range.endInclusive
  val keptSec = (endSec - startSec).coerceAtLeast(0f)
  val isWholeFile = startSec <= 0f && endSec >= durationSec
  val canTrim = keptSec >= 1f && !isWholeFile

  // Build a frame previewer off the main thread; release it when the sheet leaves composition.
  var previewer by remember { mutableStateOf<FramePreviewer?>(null) }
  DisposableEffect(previewUri, previewPath) {
    val job = scope.launch(Dispatchers.IO) {
      val built = previewUri?.let { FramePreviewer.create(context, it, previewPath ?: "", previewName) }
      if (isActive) previewer = built else built?.release()
    }
    onDispose {
      job.cancel()
      previewer?.release()
      previewer = null
    }
  }

  // Decode the tracked frame, debounced so rapid dragging doesn't queue a decode per pixel.
  var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
  LaunchedEffect(previewer, previewPosMs) {
    val p = previewer ?: return@LaunchedEffect
    delay(90)
    // Mirror the trim's keyframe behaviour: the start snaps to the previous keyframe.
    val option =
      if (activeHandleStart) {
        MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
      } else {
        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
      }
    val bmp = withContext(Dispatchers.IO) { p.frameAt(previewPosMs, option) }
    if (bmp != null) previewBitmap = bmp
  }

  PlayerSheet(onDismissRequest) {
    Column(
      modifier =
        modifier
          .verticalScroll(rememberScrollState())
          .padding(MaterialTheme.spacing.medium),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
      Text(
        text = "Trim Video",
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "Crop the start and/or end. The kept range is written back to the same file.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Frame preview of the handle currently being dragged.
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
      ) {
        val bmp = previewBitmap
        if (bmp != null) {
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
          )
        } else {
          CircularProgressIndicator()
        }
        Surface(
          color = Color.Black.copy(alpha = 0.55f),
          shape = MaterialTheme.shapes.small,
          modifier =
            Modifier
              .align(Alignment.BottomStart)
              .padding(MaterialTheme.spacing.small),
        ) {
          Text(
            text =
              (if (activeHandleStart) "Start " else "End ") +
                Utils.prettyTime((previewPosMs / 1000L).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Start ${Utils.prettyTime(startSec.toInt())}",
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = "End ${Utils.prettyTime(endSec.toInt())}",
          style = MaterialTheme.typography.titleSmall,
        )
      }

      RangeSlider(
        value = range,
        onValueChange = { newRange ->
          // Track whichever handle moved so the preview shows that endpoint.
          val movedStart = newRange.start != range.start
          activeHandleStart = movedStart
          previewPosMs = ((if (movedStart) newRange.start else newRange.endInclusive) * 1000).toLong()
          range = newRange
        },
        valueRange = 0f..durationSec,
      )

      Text(
        text = "Keeps ${Utils.prettyTime(keptSec.toInt())} of ${Utils.prettyTime(durationSec.toInt())}",
        style = MaterialTheme.typography.bodyMedium,
      )

      Text(
        text = "The start snaps to the nearest keyframe. This overwrites the original file and " +
          "can't be undone.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onDismissRequest) {
          Text("Cancel")
        }
        Button(
          enabled = canTrim,
          onClick = { showConfirm = true },
        ) {
          Text("Trim")
        }
      }
    }
  }

  if (showConfirm) {
    AlertDialog(
      onDismissRequest = { showConfirm = false },
      title = { Text("Trim video?") },
      text = {
        Text(
          "The original file will be replaced with the trimmed version " +
            "(${Utils.prettyTime(keptSec.toInt())}). This can't be undone.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showConfirm = false
            onTrim((startSec * 1000).toLong(), (endSec * 1000).toLong())
          },
        ) {
          Text("Trim")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

/** Modal, non-dismissible progress overlay shown while a trim is running. */
@Composable
fun TrimProgressDialog(progress: Float) {
  Dialog(
    onDismissRequest = {},
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
  ) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier
          .width(280.dp)
          .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          text = "Trimming video…",
          style = MaterialTheme.typography.titleMedium,
        )
        if (progress > 0f) {
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
          )
          Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }
}
