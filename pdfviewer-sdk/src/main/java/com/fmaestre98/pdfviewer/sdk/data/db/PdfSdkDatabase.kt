package com.fmaestre98.pdfviewer.sdk.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fmaestre98.pdfviewer.sdk.data.dao.SdkBookmarkDao
import com.fmaestre98.pdfviewer.sdk.data.dao.SdkHighlightDao
import com.fmaestre98.pdfviewer.sdk.data.dao.SdkPageNoteDao
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkBookmarkEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkHighlightEntity
import com.fmaestre98.pdfviewer.sdk.data.entity.SdkPageNoteEntity

@Database(
    entities = [
        SdkBookmarkEntity::class,
        SdkHighlightEntity::class,
        SdkPageNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PdfSdkDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): SdkBookmarkDao
    abstract fun highlightDao(): SdkHighlightDao
    abstract fun pageNoteDao(): SdkPageNoteDao

    companion object {
        @Volatile
        private var INSTANCE: PdfSdkDatabase? = null

        fun getInstance(context: Context): PdfSdkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfSdkDatabase::class.java,
                    "pdf_sdk_internal.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
