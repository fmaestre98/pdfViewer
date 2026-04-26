package com.fmaestre98.pdfviewer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

@HiltAndroidApp
class PdfViewerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this.applicationContext)
    }
}