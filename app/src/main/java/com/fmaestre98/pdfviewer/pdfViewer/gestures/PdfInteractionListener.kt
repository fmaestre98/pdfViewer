package com.fmaestre98.pdfviewer.pdfViewer.gestures

import androidx.compose.ui.geometry.Offset

/**
 * Interface for handling PDF viewer interactions.
 * Separates gesture detection from business logic.
 */
interface PdfInteractionListener {
    /**
     * Called when a page is tapped.
     * @param x X coordinate in screen space
     * @param y Y coordinate in screen space
     */
    fun onPageTapped(x: Float, y: Float)

    /**
     * Called when a page is long-pressed.
     * @param x X coordinate in screen space
     * @param y Y coordinate in screen space
     */
    suspend fun onPageLongPressed(x: Float, y: Float)

    /**
     * Called when a selection handle is dragged.
     * @param isStart True if dragging the start handle, false for end handle
     * @param x Absolute X coordinate in screen space
     * @param y Absolute Y coordinate in screen space
     * @param viewWidth Width of the view/overlay
     * @param viewHeight Height of the view/overlay
     */
    fun onSelectionHandleDragged(isStart: Boolean, x: Float, y: Float, viewWidth: Int, viewHeight: Int)

    /**
     * Called when a transform gesture (pinch zoom/pan) is detected.
     * @param pan Pan offset
     * @param zoom Zoom factor
     */
    fun onTransformGesture(pan: Offset, zoom: Float)

    /**
     * Called when a double-tap is detected.
     * @param x X coordinate in screen space
     * @param y Y coordinate in screen space
     */
    fun onDoubleTap(x: Float, y: Float)
    
    /**
     * Called when a transform gesture starts (zoom/pan begins).
     * Optional: default implementation does nothing.
     */
    fun onTransformStarted() {
        // Default: no-op
    }
    
    /**
     * Called when a transform gesture ends (zoom/pan ends).
     * Optional: default implementation does nothing.
     */
    fun onTransformEnded() {
        // Default: no-op
    }
    
    /**
     * Called when a selection handle drag starts.
     * Optional: default implementation does nothing.
     */
    fun onHandleDragStarted() {
        // Default: no-op
    }
    
    /**
     * Called when a selection handle drag ends.
     * Optional: default implementation does nothing.
     */
    fun onHandleDragEnded() {
        // Default: no-op
    }
}

