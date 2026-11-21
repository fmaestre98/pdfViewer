package com.fmaestre98.pdfviewer.sdk.core.model

internal data class HighlightData(
    val groupId: String,
    val color: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val snippet: String = ""
)