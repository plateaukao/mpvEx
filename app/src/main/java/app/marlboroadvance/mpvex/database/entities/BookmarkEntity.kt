package app.marlboroadvance.mpvex.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bookmarked moment inside a video: which file, the exact playback time, and a
 * thumbnail of the frame. A bookmark may optionally belong to a single [TagEntity];
 * deleting that tag leaves the bookmark untagged (FK SET NULL).
 */
@Entity(
  foreignKeys = [
    ForeignKey(
      entity = TagEntity::class,
      parentColumns = ["id"],
      childColumns = ["tagId"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [Index("tagId"), Index("mediaIdentifier"), Index("createdAt")],
)
data class BookmarkEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val videoUri: String, // uri.toString() — how to re-open (content://, file://, http://)
  val videoPath: String, // best-effort filesystem path / uri (display + tie-break)
  val fileName: String, // display name
  val mediaIdentifier: String, // stable per-video key (matches PlaybackState logic)
  val positionMs: Long, // bookmarked playback time in milliseconds
  val durationMs: Long, // total video duration in milliseconds (for progress display)
  val thumbnailPath: String? = null, // filesDir/bookmarks/<id>.jpg, null if capture failed
  val note: String? = null, // optional user label
  val tagId: Int? = null, // optional single tag
  val createdAt: Long,
)
