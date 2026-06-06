package app.marlboroadvance.mpvex.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.marlboroadvance.mpvex.database.entities.BookmarkEntity
import app.marlboroadvance.mpvex.database.entities.TagEntity
import kotlinx.coroutines.flow.Flow

/** A tag together with how many bookmarks reference it (for the tag list UI). */
data class TagWithCount(
  @Embedded val tag: TagEntity,
  val cnt: Int,
)

@Dao
interface BookmarkDao {
  // Bookmark operations
  @Insert
  suspend fun insertBookmark(bookmark: BookmarkEntity): Long

  @Update
  suspend fun updateBookmark(bookmark: BookmarkEntity)

  @Delete
  suspend fun deleteBookmark(bookmark: BookmarkEntity)

  @Query("UPDATE BookmarkEntity SET thumbnailPath = :path WHERE id = :id")
  suspend fun updateThumbnailPath(id: Int, path: String?)

  @Query("UPDATE BookmarkEntity SET tagId = :tagId WHERE id = :id")
  suspend fun setBookmarkTag(id: Int, tagId: Int?)

  @Query("SELECT * FROM BookmarkEntity WHERE id = :id")
  suspend fun getBookmarkById(id: Int): BookmarkEntity?

  @Query("SELECT * FROM BookmarkEntity ORDER BY createdAt DESC")
  fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

  @Query("SELECT * FROM BookmarkEntity WHERE tagId = :tagId ORDER BY createdAt ASC")
  fun observeBookmarksForTag(tagId: Int): Flow<List<BookmarkEntity>>

  @Query("SELECT * FROM BookmarkEntity WHERE tagId = :tagId ORDER BY createdAt ASC")
  suspend fun getBookmarksForTag(tagId: Int): List<BookmarkEntity>

  @Query("SELECT * FROM BookmarkEntity WHERE tagId IS NULL ORDER BY createdAt DESC")
  fun observeUntaggedBookmarks(): Flow<List<BookmarkEntity>>

  @Query("SELECT * FROM BookmarkEntity WHERE tagId IS NULL ORDER BY createdAt DESC")
  suspend fun getUntaggedBookmarks(): List<BookmarkEntity>

  @Query("SELECT * FROM BookmarkEntity ORDER BY createdAt DESC")
  suspend fun getAllBookmarks(): List<BookmarkEntity>

  // Tag operations
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertTag(tag: TagEntity): Long

  @Update
  suspend fun updateTag(tag: TagEntity)

  @Delete
  suspend fun deleteTag(tag: TagEntity)

  @Query("SELECT * FROM TagEntity WHERE name = :name LIMIT 1")
  suspend fun getTagByName(name: String): TagEntity?

  @Query("SELECT * FROM TagEntity WHERE id = :id")
  suspend fun getTagById(id: Int): TagEntity?

  @Query("SELECT * FROM TagEntity ORDER BY name ASC")
  fun observeAllTags(): Flow<List<TagEntity>>

  @Query(
    """
    SELECT t.*, COUNT(b.id) AS cnt FROM TagEntity t
    LEFT JOIN BookmarkEntity b ON b.tagId = t.id
    GROUP BY t.id
    ORDER BY t.name ASC
    """,
  )
  fun observeTagsWithCounts(): Flow<List<TagWithCount>>

  @Query("SELECT COUNT(*) FROM BookmarkEntity")
  fun observeBookmarkCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM BookmarkEntity WHERE tagId IS NULL")
  fun observeUntaggedCount(): Flow<Int>
}
