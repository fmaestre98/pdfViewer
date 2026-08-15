package com.fmaestre98.pdfviewer.repository

import com.fmaestre98.pdfviewer.models.Book
import com.fmaestre98.pdfviewer.room.BookDao
import com.fmaestre98.pdfviewer.room.toBook
import com.fmaestre98.pdfviewer.room.toBookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class RoomPdfRepository @Inject constructor(
    private val bookDao: BookDao,
    @ApplicationContext private val context: Context
) : PDFRepository {

    private val storageDir: File by lazy {
        File(context.filesDir, "pdfs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAllBooks().map { entities -> entities.map { it.toBook() } }

    override suspend fun getAllBooks(): List<Book> =
        bookDao.getAllBooks().map { it.toBook() }

    override suspend fun addBook(sourceUri: Uri, displayName: String, sizeBytes: Long): Book {
        // Check for duplicate books before creating a new file entry
        val existingBook = findExistingBook(sourceUri, displayName, sizeBytes)
        if (existingBook != null) {
            return existingBook
        }

        val fileName = UUID.randomUUID().toString() + ".pdf"
        val internalFile = File(storageDir, fileName)

        // Copy from source URI to internal storage
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(internalFile).use { output ->
                input.copyTo(output)
            }
        }

        val fileUriString = Uri.fromFile(internalFile).toString()

        val newBook = Book(
            uri = fileUriString,
            displayName = displayName,
            filePath = internalFile.absolutePath,
            sizeBytes = internalFile.length()
        )
        bookDao.insertBook(newBook.toBookEntity())
        return newBook
    }

    private suspend fun findExistingBook(sourceUri: Uri, displayName: String, sizeBytes: Long): Book? {
        val allBooks = getAllBooks()

        // 1. Try matching candidate by displayName and sizeBytes (if sizeBytes > 0)
        val candidate = allBooks.firstOrNull { book ->
            val sameName = book.displayName.equals(displayName, ignoreCase = true)
            val sameSize = sizeBytes > 0 && book.sizeBytes == sizeBytes
            val fileExists = book.filePath?.let { File(it).exists() } == true
            sameName && sameSize && fileExists
        }

        if (candidate != null) {
            val file = File(candidate.filePath!!)
            if (isSameFileHeader(sourceUri, file)) {
                return candidate
            }
        }

        // 2. Fallback: match by displayName and file header if size was unknown (-1)
        val nameOnlyCandidates = allBooks.filter { book ->
            book.displayName.equals(displayName, ignoreCase = true) &&
                    book.filePath?.let { File(it).exists() } == true
        }

        for (book in nameOnlyCandidates) {
            val file = File(book.filePath!!)
            if (isSameFileHeader(sourceUri, file)) {
                return book
            }
        }

        return null
    }

    private fun isSameFileHeader(sourceUri: Uri, existingFile: File): Boolean {
        return try {
            val buf1 = ByteArray(4096)
            val buf2 = ByteArray(4096)
            var read1 = 0
            var read2 = 0
            context.contentResolver.openInputStream(sourceUri)?.use { input1 ->
                read1 = input1.read(buf1)
            }
            java.io.FileInputStream(existingFile).use { input2 ->
                read2 = input2.read(buf2)
            }
            if (read1 > 0 && read1 == read2) {
                buf1.copyOf(read1).contentEquals(buf2.copyOf(read2))
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateBook(book: Book) {
        bookDao.updateBook(book.toBookEntity())
    }

    override suspend fun getBookByUri(bookUri: String): Book? =
        bookDao.getBookByUri(bookUri)?.toBook()

    override suspend fun updateLastReadPage(bookUri: String, page: Int) {
        bookDao.updateLastReadPage(bookUri, page)
    }

    override suspend fun getLastReadPage(bookUri: String): Int =
        bookDao.getBookByUri(bookUri)?.lastReadPage ?: 0

    override suspend fun deleteBook(bookUri: String) {
        val book = bookDao.getBookByUri(bookUri)
        if (book != null && book.filePath != null) {
            val file = File(book.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        bookDao.deleteBookByUri(bookUri)
    }
}
