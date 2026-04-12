package com.fmaestre98.pdfviewer.pdfViewer.gestures
import android.os.SystemClock
import com.fmaestre98.pdfviewer.pdfViewer.viewmodel.PdfInteractiveState
import androidx.compose.ui.geometry.Offset

/**
 * Orchestrator that manages gesture priorities in the PDF viewer.
 * Uses a single ActiveGesture enum to enforce mutually exclusive states.
 * 
 * Priority order:
 * 1. HANDLE_DRAGGING (Highest)
 * 2. ZOOMING
 * 3. PANNING
 * 4. NONE
 */
class GestureOrchestrator {
    // Enum representing mutually exclusive gesture states
    enum class ActiveGesture {
        NONE,
        PANNING,
        ZOOMING,
        HANDLE_DRAGGING
    }
    
    // Usage: Currently active gesture
    private var currentGesture = ActiveGesture.NONE
    
    // Timestamp when a handle drag last ended (to prevent tap-dismiss right after drag)
    private var handleDragEndedAtMs = 0L
    
    // Grace period after handle drag ends during which taps should NOT dismiss selection
    private val HANDLE_DRAG_GRACE_MS = 200L
    
    // Thresholds for gesture detection
    private val ZOOM_THRESHOLD = 0.05f
    
    /**
     * Determines if a transform gesture (pinch zoom/pan) should be processed.
     * Blocks transform if a higher priority gesture (handle drag) is active.
     */
    fun shouldProcessTransform(
        interactiveState: PdfInteractiveState,
        zoomDelta: Float,
        panDelta: Offset
    ): Boolean {
        // Handle dragging blocks everything else
        if (currentGesture == ActiveGesture.HANDLE_DRAGGING) {
            return false
        }
        return true
    }
    
    /**
     * Called when a transform gesture starts.
     * Updates state based on whether it's primarily a zoom or pan.
     */
    fun onTransformStarted(zoomDelta: Float = 1.0f) {
        // Don't interrupt handle dragging
        if (currentGesture == ActiveGesture.HANDLE_DRAGGING) return
        
        val isZoomGesture = kotlin.math.abs(zoomDelta - 1.0f) > ZOOM_THRESHOLD
        
        currentGesture = if (isZoomGesture) {
            ActiveGesture.ZOOMING
        } else {
            ActiveGesture.PANNING
        }
        
        println("my-logs onTransformStarted: $currentGesture")
    }
    
    /**
     * Called when a transform gesture ends.
     */
    fun onTransformEnded() {
        if (currentGesture == ActiveGesture.ZOOMING || currentGesture == ActiveGesture.PANNING) {
            println("my-logs onTransformEnded: Resetting to NONE")
            currentGesture = ActiveGesture.NONE
        }
    }
    
    /**
     * Determines if a tap gesture should be processed.
     */
    fun shouldProcessTap(
        interactiveState: PdfInteractiveState,
        tapPosition: Offset
    ): Boolean {
        return currentGesture == ActiveGesture.NONE
    }
    
    /**
     * Determines if a tap should dismiss the current text selection.
     * Returns false if:
     * - Any gesture is currently active (zoom/pan/handle drag)
     * - A handle drag ended very recently (within grace period)
     */
    fun shouldDismissSelectionOnTap(): Boolean {
        if (currentGesture != ActiveGesture.NONE) return false
        
        // Don't dismiss if a handle drag ended very recently
        val elapsed = SystemClock.elapsedRealtime() - handleDragEndedAtMs
        if (elapsed < HANDLE_DRAG_GRACE_MS) return false
        
        return true
    }
    
    /**
     * Determines if a long press should be processed for text selection.
     */
    fun shouldProcessLongPress(interactiveState: PdfInteractiveState): Boolean {
        // Only allow long press if no other gesture is active
        return currentGesture == ActiveGesture.NONE
    }
    
    /**
     * Called when handle drag starts.
     * Takes absolute priority.
     */
    fun onHandleDragStarted() {
        println("my-logs onHandleDragStarted: state -> HANDLE_DRAGGING")
        currentGesture = ActiveGesture.HANDLE_DRAGGING
    }
    
    /**
     * Called when handle drag ends.
     */
    fun onHandleDragEnded() {
        if (currentGesture == ActiveGesture.HANDLE_DRAGGING) {
            println("my-logs onHandleDragEnded: state -> NONE")
            currentGesture = ActiveGesture.NONE
            handleDragEndedAtMs = SystemClock.elapsedRealtime()
        }
    }
    
    /**
     * Checks if a gesture is currently active (zoom or pan or drag).
     */
    fun isGestureActive(): Boolean {
        return currentGesture != ActiveGesture.NONE
    }
    
    /**
     * Checks if handle is being dragged.
     */
    fun isHandleDragActive(): Boolean {
        return currentGesture == ActiveGesture.HANDLE_DRAGGING
    }
    
    /**
     * Resets all gesture states.
     */
    fun reset() {
        currentGesture = ActiveGesture.NONE
        handleDragEndedAtMs = 0L
    }
}
