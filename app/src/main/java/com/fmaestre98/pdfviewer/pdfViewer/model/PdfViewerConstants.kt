package com.fmaestre98.pdfviewer.pdfViewer.model

/**
 * Constants used across PDF viewer components.
 */
internal object PdfViewerConstants {
    // Zoom configuration
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 5f
    const val ZOOM_LEVEL_1 = 1f
    const val ZOOM_LEVEL_2 = 2f
    const val ZOOM_LEVEL_3 = 3f
    
    // Animation configuration
    const val ZOOM_ANIMATION_DURATION_MS = 300
    
    // Pan/Scroll configuration
    const val PAN_SENSITIVITY_FACTOR = 1.2f
    
    // Page spacing
    const val VERTICAL_PAGE_SPACING_DP = 4
    
    // Swipe indicator padding
    const val SWIPE_INDICATOR_BOTTOM_PADDING_DP = 49
    const val SWIPE_INDICATOR_TOP_PADDING_DP = 24
    
    // Page model loading debounce
    const val PAGE_MODEL_LOAD_DEBOUNCE_MS = 300L
}

