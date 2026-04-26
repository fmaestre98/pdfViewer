package com.fmaestre98.pdfviewer.ui.screens.home

import android.net.Uri

sealed interface HomeAction {
    data class OnPdfSelected(val uri: Uri, val displayName: String, val sizeBytes: Long) : HomeAction
    data class OnBookClick(val uri: String) : HomeAction
    data class OnDeleteBook(val uri: String) : HomeAction
}
