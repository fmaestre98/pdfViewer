package com.fmaestre98.pdfviewer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.fmaestre98.pdfviewer.room.AppDatabase
import com.fmaestre98.pdfviewer.room.BookDao
import com.fmaestre98.pdfviewer.room.BookmarkDao
import com.fmaestre98.pdfviewer.room.HighlightDao
import com.fmaestre98.pdfviewer.room.PageNoteDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao =
        database.bookDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao =
        database.bookmarkDao()

    @Provides
    @Singleton
    fun provideHighlightDao(database: AppDatabase): HighlightDao =
        database.highlightDao()

    @Provides
    @Singleton
    fun providePageNoteDao(database: AppDatabase): PageNoteDao =
        database.pageNoteDao()
}