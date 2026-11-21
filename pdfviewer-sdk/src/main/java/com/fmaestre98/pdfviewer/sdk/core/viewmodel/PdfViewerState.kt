package com.fmaestre98.pdfviewer.sdk.core.viewmodel

import com.fmaestre98.pdfviewer.sdk.core.model.PageModel
import com.fmaestre98.pdfviewer.sdk.core.model.PdfWord
import com.fmaestre98.pdfviewer.sdk.core.rendering.PdfRendererManager

/**
 * State class for managing PDF viewer state.
 * Composes PdfLoaderState, PdfNavigationState, and PdfInteractiveState for backward compatibility.
 *
 * @deprecated This class is maintained for backward compatibility. New code should use
 * the individual state classes (PdfLoaderState, PdfNavigationState, PdfInteractiveState) directly.
 */
internal open class PdfViewerState {
    // Compose the three focused state classes
    val loaderState = PdfLoaderState()
    val navigationState = PdfNavigationState()
    val interactiveState = PdfInteractiveState()

    open var zoomLevel: Float
        get() = interactiveState.zoomLevel
        set(value) {
            interactiveState.zoomLevel = value
        }

    open var offsetX: Float
        get() = interactiveState.offsetX
        set(value) {
            interactiveState.offsetX = value
        }

    open var offsetY: Float
        get() = interactiveState.offsetY
        set(value) {
            interactiveState.offsetY = value
        }

    open var error: Throwable?
        get() = loaderState.error
        set(value) {
            loaderState.setErrorExtra(value)
        }

    open var isTextSelectionActive: Boolean
        get() = interactiveState.isTextSelectionActive
        set(value) {
            if (value) {
                // Cannot directly set, use activateTextSelection
            } else {
                interactiveState.deactivateTextSelection()
            }
        }
    /**
     * Cleans up resources.
     */
    fun dispose() {
        loaderState.dispose()
        navigationState.reset()
        interactiveState.reset()
    }
}