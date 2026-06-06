package app.marlboroadvance.mpvex.ui.browser.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.repository.BookmarkRepository
import app.marlboroadvance.mpvex.ui.theme.spacing
import org.koin.compose.koinInject

/**
 * A compact "Bookmarks" section for the Playlists tab: a header plus a horizontal row of
 * tag chips (All, Untagged, and each tag). Each chip opens a [BookmarkTagScreen] which plays
 * its bookmarks as a queue. Hidden entirely when there are no bookmarks and no tags.
 */
@Composable
fun BookmarkTagsSection(
  onOpenTag: (BookmarkTagScreen) -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = koinInject<BookmarkRepository>()
  val tags by remember { repository.observeTagsWithCounts() }.collectAsState(initial = emptyList())
  val totalCount by remember { repository.observeBookmarkCount() }.collectAsState(initial = 0)
  val untaggedCount by remember { repository.observeUntaggedCount() }.collectAsState(initial = 0)

  if (totalCount == 0 && tags.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "Bookmarks",
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.padding(
        start = MaterialTheme.spacing.medium,
        end = MaterialTheme.spacing.medium,
        top = MaterialTheme.spacing.small,
        bottom = MaterialTheme.spacing.extraSmall,
      ),
    )
    LazyRow(
      contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
      item {
        AssistChip(
          onClick = { onOpenTag(BookmarkTagScreen(tagId = null, untaggedOnly = false, title = "All bookmarks")) },
          label = { Text("All ($totalCount)") },
          leadingIcon = {
            Icon(
              Icons.Filled.Bookmark,
              contentDescription = null,
              modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
          },
        )
      }
      if (untaggedCount > 0) {
        item {
          AssistChip(
            onClick = { onOpenTag(BookmarkTagScreen(tagId = null, untaggedOnly = true, title = "Untagged")) },
            label = { Text("Untagged ($untaggedCount)") },
          )
        }
      }
      items(tags, key = { it.tag.id }) { tagWithCount ->
        AssistChip(
          onClick = {
            onOpenTag(
              BookmarkTagScreen(
                tagId = tagWithCount.tag.id,
                untaggedOnly = false,
                title = tagWithCount.tag.name,
              ),
            )
          },
          label = { Text("${tagWithCount.tag.name} (${tagWithCount.cnt})") },
        )
      }
    }
  }
}
