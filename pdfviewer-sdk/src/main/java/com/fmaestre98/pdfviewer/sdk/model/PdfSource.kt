package com.fmaestre98.pdfviewer.sdk.model

import android.net.Uri
import java.io.File

/**
 * Representa las posibles fuentes desde donde se puede cargar un archivo PDF en el visor.
 */
sealed interface PdfSource {
    /** Carga el PDF a partir de una URI de Android (content:// o file://) */
    data class FromUri(val uri: Uri) : PdfSource
    
    /** Carga el PDF directamente desde un objeto File del sistema de archivos */
    data class FromFile(val file: File) : PdfSource
    
    /** Carga el PDF desde la carpeta assets del proyecto */
    data class FromAsset(val assetPath: String) : PdfSource

    /** Obtiene un identificador en String único para gestionar las anotaciones de este documento */
    fun getDocumentId(): String = when (this) {
        is FromUri -> uri.toString()
        is FromFile -> file.absolutePath
        is FromAsset -> "asset://$assetPath"
    }
}
