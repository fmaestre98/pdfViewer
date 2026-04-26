package com.fmaestre98.pdfviewer.repository

import com.fmaestre98.pdfviewer.models.Book
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the user's PDF library.
 *
 * Books are identified by their content [uri] (e.g. a SAF content:// URI).
 * All operations that need to target a specific book accept [bookUri] as key.
 */
interface PDFRepository {

    /** Reactively observe the full library, ordered by most recently added. */
    fun observeBooks(): Flow<List<Book>>

    /** One-shot fetch of the full library (useful for initialisation checks). */
    suspend fun getAllBooks(): List<Book>

    /** Add a newly picked PDF to the library. Ignored if the URI already exists. */
    suspend fun addBook(book: Book)

    /**
     * Update metadata fields after the document has been opened for the first time
     * (e.g. [Book.totalPages], [Book.displayName]).
     */
    suspend fun updateBook(book: Book)

    /** Find a book by its content URI, or null if not in the library. */
    suspend fun getBookByUri(bookUri: String): Book?

    /** Persist the last page the user was reading (zero-based index). */
    suspend fun updateLastReadPage(bookUri: String, page: Int)

    /** Retrieve the last page the user was reading (zero-based; 0 if never saved). */
    suspend fun getLastReadPage(bookUri: String): Int

    /** Remove a book from the library (cascades to bookmarks, highlights, notes). */
    suspend fun deleteBook(bookUri: String)
}