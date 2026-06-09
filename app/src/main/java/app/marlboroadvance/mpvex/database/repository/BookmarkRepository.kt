package app.marlboroadvance.mpvex.database.repository

import app.marlboroadvance.mpvex.database.dao.BookmarkDao
import app.marlboroadvance.mpvex.database.dao.TagWithCount
import app.marlboroadvance.mpvex.database.entities.BookmarkEntity
import app.marlboroadvance.mpvex.database.entities.TagEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
  // Bookmark operations
  suspend fun createBookmark(
    videoUri: String,
    videoPath: String,
    fileName: String,
    mediaIdentifier: String,
    positionMs: Long,
    durationMs: Long,
    tagId: Int?,
    note: String? = null,
  ): Long {
    val id = bookmarkDao.insertBookmark(
      BookmarkEntity(
        videoUri = videoUri,
        videoPath = videoPath,
        fileName = fileName,
        mediaIdentifier = mediaIdentifier,
        positionMs = positionMs,
        durationMs = durationMs,
        thumbnailPath = null,
        note = note,
        tagId = tagId,
        createdAt = System.currentTimeMillis(),
      ),
    )
    // Merge near-duplicates: a same-video, same-tag bookmark within DEDUP_WINDOW_MS of this
    // one marks the same moment — drop the older one(s) and keep the new bookmark.
    bookmarkDao
      .findNearbyBookmarks(mediaIdentifier, id.toInt(), tagId, positionMs, DEDUP_WINDOW_MS)
      .forEach { deleteBookmark(it) }
    return id
  }

  suspend fun updateThumbnailPath(id: Int, path: String?) = bookmarkDao.updateThumbnailPath(id, path)

  suspend fun setBookmarkTag(id: Int, tagId: Int?) = bookmarkDao.setBookmarkTag(id, tagId)

  suspend fun getBookmarkById(id: Int): BookmarkEntity? = bookmarkDao.getBookmarkById(id)

  /** Delete a bookmark and its thumbnail file (orphan cleanup). */
  suspend fun deleteBookmark(bookmark: BookmarkEntity) {
    bookmark.thumbnailPath?.let { path ->
      runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
    bookmarkDao.deleteBookmark(bookmark)
  }

  fun observeAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAllBookmarks()

  fun observeBookmarksForTag(tagId: Int): Flow<List<BookmarkEntity>> = bookmarkDao.observeBookmarksForTag(tagId)

  suspend fun getBookmarksForTag(tagId: Int): List<BookmarkEntity> = bookmarkDao.getBookmarksForTag(tagId)

  fun observeUntaggedBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.observeUntaggedBookmarks()

  suspend fun getUntaggedBookmarks(): List<BookmarkEntity> = bookmarkDao.getUntaggedBookmarks()

  suspend fun getAllBookmarks(): List<BookmarkEntity> = bookmarkDao.getAllBookmarks()

  // Tag operations
  suspend fun getOrCreateTag(name: String): Int {
    val trimmed = name.trim()
    bookmarkDao.getTagByName(trimmed)?.let { return it.id }
    val id = bookmarkDao.insertTag(TagEntity(name = trimmed, createdAt = System.currentTimeMillis()))
    // insertTag may return -1 on IGNORE conflict (race); re-read by name in that case.
    return if (id > 0) id.toInt() else bookmarkDao.getTagByName(trimmed)?.id ?: -1
  }

  suspend fun getTagById(id: Int): TagEntity? = bookmarkDao.getTagById(id)

  suspend fun deleteTag(tag: TagEntity) = bookmarkDao.deleteTag(tag)

  fun observeAllTags(): Flow<List<TagEntity>> = bookmarkDao.observeAllTags()

  fun observeTagsWithCounts(): Flow<List<TagWithCount>> = bookmarkDao.observeTagsWithCounts()

  fun observeBookmarkCount(): Flow<Int> = bookmarkDao.observeBookmarkCount()

  fun observeUntaggedCount(): Flow<Int> = bookmarkDao.observeUntaggedCount()

  /**
   * Remove thumbnail files in [bookmarksDir] that have no matching bookmark id.
   * Bounded startup sweep; safe to call off the main thread.
   */
  suspend fun sweepOrphanThumbnails(bookmarksDir: File) {
    if (!bookmarksDir.isDirectory) return
    val validIds = getAllBookmarks().mapNotNull { it.id }.toHashSet()
    bookmarksDir.listFiles()?.forEach { file ->
      val id = file.nameWithoutExtension.toIntOrNull()
      if (id == null || id !in validIds) {
        runCatching { file.delete() }
      }
    }
  }

  companion object {
    /** A new bookmark within this window of an existing same-video, same-tag bookmark replaces it. */
    private const val DEDUP_WINDOW_MS = 20_000L
  }
}
