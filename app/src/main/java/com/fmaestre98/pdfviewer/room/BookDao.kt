package com.fmaestre98.pdfviewer.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fmaestre98.pdfviewer.room.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /** Insert a new book. If a book with the same [uri] already exists, ignore the insert. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: BookEntity): Long

    /** Observe all books ordered by most recently added. */
    @Query(BookQueries.GET_ALL_BOOKS)
    fun observeAllBooks(): Flow<List<BookEntity>>

    /** One-shot fetch of all books. */
    @Query(BookQueries.GET_ALL_BOOKS)
    suspend fun getAllBooks(): List<BookEntity>

    /** Find a book by its content URI. */
    @Query(BookQueries.GET_BOOK_BY_URI)
    suspend fun getBookByUri(uri: String): BookEntity?

    /** Update metadata (totalPages, displayName, filePath, sizeBytes) after first open. */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateBook(book: BookEntity)

    /** Persist the last page the user was reading. */
    @Query(BookQueries.UPDATE_LAST_READ_PAGE)
    suspend fun updateLastReadPage(bookUri: String, page: Int)

    /** Remove a book (cascades to bookmarks, highlights and notes). */
    @Query(BookQueries.DELETE_BOOK_BY_URI)
    suspend fun deleteBookByUri(uri: String)
}
