package com.fmaestre98.pdfviewer.repository

import kotlinx.coroutines.flow.Flow
import com.fmaestre98.pdfviewer.room.BookmarkDao
import com.fmaestre98.pdfviewer.room.entity.BookmarkEntity
import javax.inject.Inject

interface BookmarkRepository {
    suspend fun addBookmark(bookUri: String, page: Int)
    suspend fun removeBookmark(bookUri: String, page: Int)
    /** Toggles the bookmark state and returns true if it is now bookmarked. */
    suspend fun toggleBookmark(bookUri: String, page: Int): Boolean
    suspend fun isPageBookmarked(bookUri: String, page: Int): Boolean
    fun getBookmarksForBook(bookUri: String): Flow<List<BookmarkEntity>>
    suspend fun getAllBookmarksForBook(bookUri: String): List<BookmarkEntity>
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>
}

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override suspend fun addBookmark(bookUri: String, page: Int) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                bookUri = bookUri,
                page = page,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeBookmark(bookUri: String, page: Int) {
        bookmarkDao.deleteBookmarkByPage(bookUri, page)
    }

    override suspend fun toggleBookmark(bookUri: String, page: Int): Boolean {
        return if (bookmarkDao.isPageBookmarked(bookUri, page)) {
            bookmarkDao.deleteBookmarkByPage(bookUri, page)
            false
        } else {
            addBookmark(bookUri, page)
            true
        }
    }

    override suspend fun isPageBookmarked(bookUri: String, page: Int): Boolean =
        bookmarkDao.isPageBookmarked(bookUri, page)

    override fun getBookmarksForBook(bookUri: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForBook(bookUri)

    override suspend fun getAllBookmarksForBook(bookUri: String): List<BookmarkEntity> =
        bookmarkDao.getAllBookmarksForBook(bookUri)

    override fun getAllBookmarks(): Flow<List<BookmarkEntity>> =
        bookmarkDao.getAllBookmarksFlow()
}