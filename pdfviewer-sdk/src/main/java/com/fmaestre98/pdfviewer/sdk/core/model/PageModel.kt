package com.fmaestre98.pdfviewer.sdk.core.model

import android.graphics.PointF

internal data class PageModel(
    val coordinates: ArrayList<PdfLine>,
    var targetWidth: Int = 0,
    var targetHeight: Int = 0,
    var relativeSizeCalculated: Boolean = false
)