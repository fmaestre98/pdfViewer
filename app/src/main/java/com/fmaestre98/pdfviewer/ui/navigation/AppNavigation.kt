package com.fmaestre98.pdfviewer.ui.navigation

object AppNavigation {
    const val ROUTE_HOME = "home"
    const val ROUTE_ANNOTATIONS = "annotations"
    const val ROUTE_READER = "reader/{encodedUri}?initialPage={initialPage}"

    fun createReaderRoute(uri: String, initialPage: Int? = null): String {
        val encodedUri = android.net.Uri.encode(uri)
        return if (initialPage != null) {
            "reader/$encodedUri?initialPage=$initialPage"
        } else {
            "reader/$encodedUri"
        }
    }
}

