package app.marlboroadvance.mpvex.ui.browser.cards

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.entities.BookmarkEntity
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import `is`.xyz.mpv.Utils
import java.io.File

/**
 * A single bookmark row: frame thumbnail, video name, timestamp, optional tag chip.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkCard(
  bookmark: BookmarkEntity,
  tagName: String?,
  isSelected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var thumbnail by remember(bookmark.id, bookmark.thumbnailPath) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(bookmark.thumbnailPath) {
    val path = bookmark.thumbnailPath
    thumbnail = if (path != null) {
      withContext(Dispatchers.IO) {
        runCatching {
          val file = File(path)
          if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }.getOrNull()
      }
    } else {
      null
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = MaterialTheme.spacing.smaller, vertical = 4.dp)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainer
      },
    ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier.padding(MaterialTheme.spacing.smaller),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .width(120.dp)
          .aspectRatio(16f / 9f)
          .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
      ) {
        val thumb = thumbnail
        if (thumb != null) {
          Image(
            bitmap = thumb,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
          )
        } else {
          Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
          )
        }
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(start = MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = bookmark.fileName,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = Utils.prettyTime((bookmark.positionMs / 1000).toInt()),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        if (!bookmark.note.isNullOrBlank()) {
          Text(
            text = bookmark.note,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (tagName != null) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
              imageVector = Icons.Filled.Sell,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = tagName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
