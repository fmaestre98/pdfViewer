package com.fmaestre98.pdfviewer.models

/**
 * Domain model representing a PDF document opened from device storage.
 *
 * @param id          Unique identifier (matches the Room primary key).
 * @param uri         Content URI string used to reopen the document (e.g. content://…).
 * @param displayName Human-readable file name shown in the home screen list.
 * @param filePath    Absolute path to the file on disk, if available (may be null for SAF-only URIs).
 * @param sizeBytes   File size in bytes; -1 if unknown.
 * @param totalPages  Total page count; 0 until it has been read at least once.
 * @param lastReadPage Zero-based index of the last page the user was on.
 * @param addedAt     Epoch-millis timestamp when the book was first added to the library.
 */
data class Book(
    val id: Long = 0,
    val uri: String,
    val displayName: String,
    val filePath: String? = null,
    val sizeBytes: Long = -1L,
    val totalPages: Int = 0,
    val lastReadPage: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)
