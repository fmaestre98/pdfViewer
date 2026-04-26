package com.fmaestre98.pdfviewer.ui.navigation

object AppNavigation {
    const val ROUTE_HOME = "home"
    const val ROUTE_READER = "reader/{encodedUri}"

    fun createReaderRoute(uri: String): String {
        // Simple encoding to pass the URI safely as a navigation argument
        val encodedUri = android.net.Uri.encode(uri)
        return "reader/$encodedUri"
    }
}
