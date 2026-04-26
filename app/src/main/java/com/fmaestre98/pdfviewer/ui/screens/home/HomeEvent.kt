package com.fmaestre98.pdfviewer.ui.screens.home

sealed interface HomeEvent {
    data class NavigateToReader(val uri: String) : HomeEvent
    data class ShowSnackbar(val messageResId: Int) : HomeEvent
}
