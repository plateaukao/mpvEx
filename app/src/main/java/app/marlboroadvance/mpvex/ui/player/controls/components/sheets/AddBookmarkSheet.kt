package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.dao.TagWithCount
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing

/**
 * Bottom sheet shown when the user taps the "Add bookmark" button. Lets them optionally
 * assign a single tag (existing or newly typed) and an optional note before saving.
 *
 * [onSave] receives the chosen existing tag id (or null), a new tag name to create (or null;
 * takes precedence when non-blank), and an optional note.
 */
@Composable
fun AddBookmarkSheet(
  tags: List<TagWithCount>,
  positionLabel: String,
  onSave: (selectedTagId: Int?, newTagName: String?, note: String?) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTagId by remember { mutableStateOf<Int?>(null) }
  var newTagText by remember { mutableStateOf("") }
  var noteText by remember { mutableStateOf("") }

  PlayerSheet(onDismissRequest) {
    Column(
      modifier =
        modifier
          .verticalScroll(rememberScrollState())
          .padding(vertical = MaterialTheme.spacing.medium),
    ) {
      Text(
        text = "Add Bookmark",
        style = MaterialTheme.typography.headlineSmall,
        modifier =
          Modifier
            .padding(horizontal = MaterialTheme.spacing.medium),
      )
      Text(
        text = "At $positionLabel",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
          Modifier
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(bottom = MaterialTheme.spacing.small),
      )

      if (tags.isNotEmpty()) {
        Text(
          text = "Tag",
          style = MaterialTheme.typography.titleSmall,
          modifier =
            Modifier
              .padding(horizontal = MaterialTheme.spacing.medium)
              .padding(top = MaterialTheme.spacing.small),
        )
        LazyRow(
          modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
          items(tags, key = { it.tag.id }) { tagWithCount ->
            FilterChip(
              selected = selectedTagId == tagWithCount.tag.id,
              onClick = {
                selectedTagId = if (selectedTagId == tagWithCount.tag.id) null else tagWithCount.tag.id
              },
              label = { Text("${tagWithCount.tag.name} (${tagWithCount.cnt})") },
              modifier = Modifier.animateItem(),
            )
          }
        }
      }

      OutlinedTextField(
        value = newTagText,
        onValueChange = { newTagText = it },
        label = { Text("New tag (optional)") },
        singleLine = true,
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(top = MaterialTheme.spacing.small),
      )
      if (newTagText.isNotBlank() && tags.isNotEmpty()) {
        Text(
          text = "A new tag will be used instead of the selected one.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier =
            Modifier
              .padding(horizontal = MaterialTheme.spacing.medium)
              .padding(top = 4.dp),
        )
      }

      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        label = { Text("Note (optional)") },
        singleLine = true,
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(top = MaterialTheme.spacing.small),
      )

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(top = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onDismissRequest) {
          Text("Cancel")
        }
        Button(
          onClick = {
            onSave(
              selectedTagId,
              newTagText.trim().takeIf { it.isNotBlank() },
              noteText.trim().takeIf { it.isNotBlank() },
            )
          },
        ) {
          Text("Save")
        }
      }
    }
  }
}
