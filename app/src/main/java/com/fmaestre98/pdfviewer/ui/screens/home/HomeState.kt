package com.fmaestre98.pdfviewer.ui.screens.home

import com.fmaestre98.pdfviewer.models.Book

data class HomeState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
