package app.marlboroadvance.mpvex.ui.browser.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.entities.BookmarkEntity
import app.marlboroadvance.mpvex.database.repository.BookmarkRepository
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.ConfirmDialog
import app.marlboroadvance.mpvex.ui.browser.cards.BookmarkCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/**
 * Lists the bookmarks belonging to a tag (or All / Untagged) and plays them as a queue.
 *
 * @param tagId the tag to show; null means all or untagged depending on [untaggedOnly]
 * @param untaggedOnly when true (and tagId is null), shows only bookmarks with no tag
 * @param title shown in the top bar
 */
@Serializable
data class BookmarkTagScreen(
  val tagId: Int? = null,
  val untaggedOnly: Boolean = false,
  val title: String = "Bookmarks",
) : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val repository = koinInject<BookmarkRepository>()
    val scope = rememberCoroutineScope()

    val bookmarksFlow = remember(tagId, untaggedOnly) {
      when {
        tagId != null -> repository.observeBookmarksForTag(tagId)
        untaggedOnly -> repository.observeUntaggedBookmarks()
        else -> repository.observeAllBookmarks()
      }
    }
    val bookmarks by bookmarksFlow.collectAsState(initial = emptyList())

    // Tag name lookup for the "All" view (where bookmarks may have different tags).
    val tags by remember { repository.observeAllTags() }.collectAsState(initial = emptyList())
    val tagNames = remember(tags) { tags.associate { it.id to it.name } }

    var pendingDelete by remember { mutableStateOf<BookmarkEntity?>(null) }

    Scaffold(
      topBar = {
        BrowserTopBar(
          title = title,
          isInSelectionMode = false,
          selectedCount = 0,
          totalCount = bookmarks.size,
          onCancelSelection = {},
          onBackClick = { backStack.removeLastOrNull() },
          additionalActions = {
            if (bookmarks.isNotEmpty()) {
              IconButton(onClick = { MediaUtils.playBookmarks(bookmarks, 0, context) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play all")
              }
            }
          },
        )
      },
    ) { padding ->
      if (bookmarks.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentAlignment = Alignment.Center,
        ) {
          EmptyState(
            icon = Icons.Filled.Bookmark,
            title = "No bookmarks",
            message = "Add bookmarks from the player to see them here",
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentPadding = PaddingValues(vertical = 8.dp),
        ) {
          items(bookmarks, key = { it.id }) { bookmark ->
            BookmarkCard(
              bookmark = bookmark,
              tagName = bookmark.tagId?.let { tagNames[it] },
              isSelected = false,
              onClick = {
                val index = bookmarks.indexOfFirst { it.id == bookmark.id }.coerceAtLeast(0)
                MediaUtils.playBookmarks(bookmarks, index, context)
              },
              onLongClick = { pendingDelete = bookmark },
            )
          }
        }
      }
    }

    pendingDelete?.let { bookmark ->
      ConfirmDialog(
        title = "Delete bookmark?",
        subtitle = "This removes the bookmark and its thumbnail. The video file is not affected.",
        onConfirm = {
          scope.launch { repository.deleteBookmark(bookmark) }
          pendingDelete = null
        },
        onCancel = { pendingDelete = null },
      )
    }
  }
}
