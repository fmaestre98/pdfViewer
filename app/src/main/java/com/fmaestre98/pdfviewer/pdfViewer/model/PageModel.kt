package com.fmaestre98.pdfviewer.pdfViewer.model

import android.graphics.PointF

data class PageModel(
    val coordinates: ArrayList<PdfLine>,
    var targetWidth: Int = 0,
    var targetHeight: Int = 0,
    var relativeSizeCalculated: Boolean = false
)