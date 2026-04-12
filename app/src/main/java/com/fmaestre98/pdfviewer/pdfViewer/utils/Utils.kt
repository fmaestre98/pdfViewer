package com.fmaestre98.pdfviewer.pdfViewer.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun getOrCopyLocalFile(context: Context, assetPath: String): File? {
    val localDir = File(context.filesDir, "pdfs")
    if (!localDir.exists()) {
        localDir.mkdirs()
    }
    val localFile = File(localDir, assetPath)
    if (localFile.exists()) {
        return localFile
    }
    try {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        }
        return localFile
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}