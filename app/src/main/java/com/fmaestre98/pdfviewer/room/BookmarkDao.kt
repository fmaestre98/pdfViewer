package com.fmaestre98.pdfviewer.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow
import com.fmaestre98.pdfviewer.room.entity.BookmarkEntity

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM pdfBookmarks WHERE book_uri = :bookUri")
    fun getBookmarksForBook(bookUri: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM pdfBookmarks WHERE book_uri = :bookUri AND page = :page")
    suspend fun getBookmark(bookUri: String, page: Int): BookmarkEntity?

    @Query("DELETE FROM pdfBookmarks WHERE book_uri = :bookUri AND page = :page")
    suspend fun deleteBookmarkByPage(bookUri: String, page: Int)

    @Query("SELECT COUNT(*) FROM pdfBookmarks WHERE book_uri = :bookUri AND page = :page")
    suspend fun isPageBookmarked(bookUri: String, page: Int): Boolean

    @Query("SELECT * FROM pdfBookmarks WHERE book_uri = :bookUri")
    suspend fun getAllBookmarksForBook(bookUri: String): List<BookmarkEntity>

    @Query("SELECT * FROM pdfBookmarks ORDER BY created_at DESC")
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>>
}