package com.fmaestre98.pdfviewer.ui.screens.reader

data class PdfReaderState(
    val uri: String = "",
    val filePath: String? = null,
    val isLoading: Boolean = true,
    val initialPage: Int = 0,
    val error: String? = null
)
