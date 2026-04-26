package com.fmaestre98.pdfviewer.pdfViewer.model

data class HighlightData(
    val groupId: String,
    val color: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val snippet: String = ""
)