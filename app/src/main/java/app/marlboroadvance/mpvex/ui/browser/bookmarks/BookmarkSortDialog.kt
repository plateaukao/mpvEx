package app.marlboroadvance.mpvex.ui.browser.bookmarks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.marlboroadvance.mpvex.preferences.BookmarkSortType
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.browser.dialogs.GridColumnSelector
import app.marlboroadvance.mpvex.ui.browser.dialogs.SortDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.ViewModeSelector
import org.koin.compose.koinInject

/**
 * Sort & view options for the bookmarks list, reusing the shared [SortDialog].
 * Sort fields are bookmark-specific; layout/grid-column settings reuse the shared video prefs.
 */
@Composable
fun BookmarkSortDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val sortType by browserPreferences.bookmarkSortType.collectAsState()
  val sortOrder by browserPreferences.bookmarkSortOrder.collectAsState()
  val mediaLayoutMode by browserPreferences.mediaLayoutMode.collectAsState()
  val videoGridColumnsPortrait by browserPreferences.videoGridColumnsPortrait.collectAsState()
  val videoGridColumnsLandscape by browserPreferences.videoGridColumnsLandscape.collectAsState()

  val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
    android.content.res.Configuration.ORIENTATION_LANDSCAPE
  val videoGridColumns = if (isLandscape) videoGridColumnsLandscape else videoGridColumnsPortrait

  val gridColumnSelector = if (mediaLayoutMode == MediaLayoutMode.GRID) {
    GridColumnSelector(
      label = "Grid Columns (${if (isLandscape) "Landscape" else "Portrait"})",
      currentValue = videoGridColumns,
      onValueChange = {
        if (isLandscape) browserPreferences.videoGridColumnsLandscape.set(it)
        else browserPreferences.videoGridColumnsPortrait.set(it)
      },
      valueRange = if (isLandscape) 3f..5f else 1f..3f,
      steps = 1,
    )
  } else {
    null
  }

  SortDialog(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = "Sort & View Options",
    sortType = sortType.displayName,
    onSortTypeChange = { typeName ->
      BookmarkSortType.entries.find { it.displayName == typeName }?.let { browserPreferences.bookmarkSortType.set(it) }
    },
    sortOrderAsc = sortOrder.isAscending,
    onSortOrderChange = { isAsc ->
      browserPreferences.bookmarkSortOrder.set(if (isAsc) SortOrder.Ascending else SortOrder.Descending)
    },
    types = listOf(
      BookmarkSortType.DateAdded.displayName,
      BookmarkSortType.Timestamp.displayName,
      BookmarkSortType.Title.displayName,
    ),
    icons = listOf(
      Icons.Filled.CalendarToday,
      Icons.Filled.AccessTime,
      Icons.Filled.Title,
    ),
    getLabelForType = { type, _ ->
      when (type) {
        BookmarkSortType.DateAdded.displayName -> Pair("Oldest", "Newest")
        BookmarkSortType.Timestamp.displayName -> Pair("Earliest", "Latest")
        BookmarkSortType.Title.displayName -> Pair("A-Z", "Z-A")
        else -> Pair("Asc", "Desc")
      }
    },
    layoutModeSelector = ViewModeSelector(
      label = "Layout",
      firstOptionLabel = "List",
      secondOptionLabel = "Grid",
      firstOptionIcon = Icons.AutoMirrored.Filled.ViewList,
      secondOptionIcon = Icons.Filled.GridView,
      isFirstOptionSelected = mediaLayoutMode == MediaLayoutMode.LIST,
      onViewModeChange = { isFirstOption ->
        browserPreferences.mediaLayoutMode.set(
          if (isFirstOption) MediaLayoutMode.LIST else MediaLayoutMode.GRID,
        )
      },
    ),
    videoGridColumnSelector = gridColumnSelector,
  )
}
