package com.fmaestre98.pdfviewer.sdk.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = SdkBookmarkEntity.TABLE_NAME,
    indices = [Index(value = [SdkBookmarkEntity.FIELD_BOOK_URI])]
)
data class SdkBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long = 0,

    @ColumnInfo(name = FIELD_BOOK_URI)
    val bookUri: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_CREATED_AT)
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TABLE_NAME = "sdk_pdf_bookmarks"

        const val FIELD_ID = "id"
        const val FIELD_BOOK_URI = "book_uri"
        const val FIELD_PAGE = "page"
        const val FIELD_CREATED_AT = "created_at"
    }
}

@Entity(
    tableName = SdkHighlightEntity.TABLE_NAME,
    indices = [
        Index(value = [SdkHighlightEntity.FIELD_BOOK_URI]),
        Index(value = [SdkHighlightEntity.FIELD_GROUP_ID])
    ]
)
data class SdkHighlightEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long = 0,

    @ColumnInfo(name = FIELD_BOOK_URI)
    val bookUri: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_SNIPPET)
    val snippet: String,

    @ColumnInfo(name = FIELD_COLOR)
    val color: String,

    @ColumnInfo(name = FIELD_START_X)
    val startX: Float,

    @ColumnInfo(name = FIELD_START_Y)
    val startY: Float,

    @ColumnInfo(name = FIELD_END_X)
    val endX: Float,

    @ColumnInfo(name = FIELD_END_Y)
    val endY: Float,

    @ColumnInfo(name = FIELD_GROUP_ID)
    val groupId: String,

    @ColumnInfo(name = FIELD_UPDATED_AT)
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TABLE_NAME = "sdk_pdf_highlights"

        const val FIELD_ID = "id"
        const val FIELD_BOOK_URI = "book_uri"
        const val FIELD_PAGE = "page"
        const val FIELD_SNIPPET = "snippet"
        const val FIELD_COLOR = "color"
        const val FIELD_START_X = "start_x"
        const val FIELD_START_Y = "start_y"
        const val FIELD_END_X = "end_x"
        const val FIELD_END_Y = "end_y"
        const val FIELD_GROUP_ID = "group_id"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}

@Entity(
    tableName = SdkPageNoteEntity.TABLE_NAME,
    indices = [
        Index(value = [SdkPageNoteEntity.FIELD_BOOK_URI, SdkPageNoteEntity.FIELD_PAGE], unique = true)
    ]
)
data class SdkPageNoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long = 0,

    @ColumnInfo(name = FIELD_BOOK_URI)
    val bookUri: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_NOTE_TEXT)
    val noteText: String,

    @ColumnInfo(name = FIELD_COLOR)
    val color: String? = null,

    @ColumnInfo(name = FIELD_UPDATED_AT)
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TABLE_NAME = "sdk_page_notes"

        const val FIELD_ID = "id"
        const val FIELD_BOOK_URI = "book_uri"
        const val FIELD_PAGE = "page"
        const val FIELD_NOTE_TEXT = "note_text"
        const val FIELD_COLOR = "color"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}
