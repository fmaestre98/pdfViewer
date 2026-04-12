package com.fmaestre98.pdfviewer.pdfViewer.viewmodel
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfRendererManager


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State class for managing PDF loading, errors, and renderer manager.
 * Handles the lifecycle of PDF file loading and rendering resources.
 */
class PdfLoaderState {
    var isLoading by mutableStateOf(false)

    var error: Throwable? by mutableStateOf(null)

    var rendererManager: PdfRendererManager? by mutableStateOf(null)

    /**
     * Page count derived from renderer manager.
     */
    val pageCount: Int
        get() = rendererManager?.pageCount ?: 0

    /**
     * Updates the renderer manager and clears loading/error states.
     */
    fun setRendererManagerExtra(manager: PdfRendererManager?) {
        rendererManager = manager
        isLoading = false
        error = null
    }

    /**
     * Sets loading state.
     */
    fun setLoadingExtra(loading: Boolean) {
        isLoading = loading
    }

    /**
     * Sets error state and clears loading.
     */
    fun setErrorExtra(throwable: Throwable?) {
        error = throwable
        isLoading = false
    }

    /**
     * Cleans up resources.
     */
    fun dispose() {
        rendererManager?.close()
        rendererManager = null
        isLoading = false
        error = null
    }
}

