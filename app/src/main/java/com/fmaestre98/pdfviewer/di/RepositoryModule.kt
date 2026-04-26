package com.fmaestre98.pdfviewer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.fmaestre98.pdfviewer.repository.BookmarkRepository
import com.fmaestre98.pdfviewer.repository.BookmarkRepositoryImpl
import com.fmaestre98.pdfviewer.repository.HighlightRepository
import com.fmaestre98.pdfviewer.repository.HighlightRepositoryImpl
import com.fmaestre98.pdfviewer.repository.PageNoteRepository
import com.fmaestre98.pdfviewer.repository.PageNoteRepositoryImpl
import com.fmaestre98.pdfviewer.repository.PDFRepository
import com.fmaestre98.pdfviewer.repository.RoomPdfRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPdfRepository(impl: RoomPdfRepository): PDFRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindHighlightRepository(impl: HighlightRepositoryImpl): HighlightRepository

    @Binds
    @Singleton
    abstract fun bindPageNoteRepository(impl: PageNoteRepositoryImpl): PageNoteRepository
}