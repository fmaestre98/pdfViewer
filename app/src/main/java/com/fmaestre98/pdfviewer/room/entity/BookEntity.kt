package com.fmaestre98.pdfviewer.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a PDF document added to the user's library.
 *
 * The [uri] column is marked unique so the same document cannot be inserted twice
 * (e.g. the user picks the same file from the picker a second time).
 *
 * Foreign-key children (BookmarkEntity, HighlightEntity, PageNoteEntity) reference
 * [uri] via a ForeignKey constraint so their data is automatically deleted when the
 * book is removed from the library.
 */
@Entity(
    tableName = BookEntity.TABLE_NAME,
    indices = [Index(value = [BookEntity.FIELD_URI], unique = true)]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long = 0,

    /** Content URI string (content://… or file://…) used to reopen the document. */
    @ColumnInfo(name = FIELD_URI)
    val uri: String,

    /** Human-readable file name shown in the home screen (e.g. "My Document.pdf"). */
    @ColumnInfo(name = FIELD_DISPLAY_NAME)
    val displayName: String,

    /** Absolute path on disk; may be null for SAF-only URIs. */
    @ColumnInfo(name = FIELD_FILE_PATH)
    val filePath: String? = null,

    /** File size in bytes; -1 when unknown. */
    @ColumnInfo(name = FIELD_SIZE_BYTES)
    val sizeBytes: Long = -1L,

    /** Total number of pages; 0 until the document has been opened at least once. */
    @ColumnInfo(name = FIELD_TOTAL_PAGES)
    val totalPages: Int = 0,

    /** Zero-based index of the last page the user was on. */
    @ColumnInfo(name = FIELD_LAST_READ_PAGE)
    val lastReadPage: Int = 0,

    /** Epoch-millis timestamp when the document was added to the library. */
    @ColumnInfo(name = FIELD_ADDED_AT)
    val addedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TABLE_NAME = "books"

        const val FIELD_ID = "id"
        const val FIELD_URI = "uri"
        const val FIELD_DISPLAY_NAME = "display_name"
        const val FIELD_FILE_PATH = "file_path"
        const val FIELD_SIZE_BYTES = "size_bytes"
        const val FIELD_TOTAL_PAGES = "total_pages"
        const val FIELD_LAST_READ_PAGE = "last_read_page"
        const val FIELD_ADDED_AT = "added_at"
    }
}
