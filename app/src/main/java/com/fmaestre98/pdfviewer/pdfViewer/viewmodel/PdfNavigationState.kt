package com.fmaestre98.pdfviewer.pdfViewer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * State class for managing PDF page navigation.
 * Handles current page index and page count tracking.
 */
class PdfNavigationState {
    var currentPageIndex by mutableIntStateOf(0)
        private set

    /**
     * Updates the current page index.
     */
    fun setCurrentPage(pageIndex: Int) {
        currentPageIndex = pageIndex
    }

    /**
     * Resets navigation state.
     */
    fun reset() {
        currentPageIndex = 0
    }
}

