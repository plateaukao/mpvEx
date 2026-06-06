package app.marlboroadvance.mpvex.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-defined tag. Each tag acts like a playlist of bookmarked moments:
 * the bookmarks sharing a tag are played in sequence, seeking to each
 * bookmark's timestamp.
 */
@Entity(indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  val color: Int? = null, // optional ARGB tint for the tag chip
  val createdAt: Long,
)
