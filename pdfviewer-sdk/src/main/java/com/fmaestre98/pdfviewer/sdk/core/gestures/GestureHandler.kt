package com.fmaestre98.pdfviewer.sdk.core.gestures

import androidx.compose.ui.geometry.Offset

/**
 * GestureHandler: Pure gesture detector that delegates to PdfInteractionListener.
 * Separates gesture detection from business logic.
 */
class GestureHandler(
    private val listener: PdfInteractionListener
) {
    /**
     * Handles transform gestures (pinch zoom and pan).
     */
    fun handleTransformGesture(pan: Offset, zoom: Float) {
        listener.onTransformGesture(pan, zoom)
    }

    /**
     * Handles double tap for zoom.
     */
    fun handleDoubleTap(tapOffset: Offset) {
        listener.onDoubleTap(tapOffset.x, tapOffset.y)
    }

    /**
     * Handles long press for word selection.
     */
    suspend fun handleLongPress(tapOffset: Offset) {
        listener.onPageLongPressed(tapOffset.x, tapOffset.y)
    }

    /**
     * Handles simple tap (to exit selection mode).
     */
    fun handleTap(tapOffset: Offset) {
        listener.onPageTapped(tapOffset.x, tapOffset.y)
    }

    /**
     * Updates selection handle position.
     * @param sourcePage The page index whose overlay is generating this event (the y coordinate origin).
     */
    fun updateSelectionHandle(isStart: Boolean, x: Float, y: Float, viewWidth: Int, viewHeight: Int, sourcePage: Int = -1) {
        listener.onSelectionHandleDragged(isStart, x, y, viewWidth, viewHeight, sourcePage)
    }
    
    /**
     * Notifies that handle drag has started.
     */
    fun notifyHandleDragStarted() {
        listener.onHandleDragStarted()
    }
    
    /**
     * Notifies that handle drag has ended.
     */
    fun notifyHandleDragEnded() {
        listener.onHandleDragEnded()
    }
    
    /**
     * Notifies that transform gesture has started.
     */
    fun notifyTransformStarted() {
        listener.onTransformStarted()
    }
    
    /**
     * Notifies that transform gesture has ended.
     */
    fun notifyTransformEnded() {
        listener.onTransformEnded()
    }
}