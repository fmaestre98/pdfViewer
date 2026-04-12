package com.fmaestre98.pdfviewer.pdfViewer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfChar
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord
import com.fmaestre98.pdfviewer.repository.HighlightData

/**
 * State class for managing PDF viewer interactions.
 * Handles zoom, pan, text selection, and current page model.
 */
class PdfInteractiveState {
    var zoomLevel by mutableFloatStateOf(1.0f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)



    var isTextSelectionActive: Boolean
        get() = selectionStartChar != null && selectionEndChar != null
        private set(value) {
            // This setter is kept for backward compatibility but doesn't block other gestures
            if (!value) {
                // Only clear selection if explicitly deactivated
                selectionStartChar = null
                selectionEndChar = null
                selectionPageIndex = -1
            }
        }
    


    // CAMBIO: Ahora rastreamos caracteres específicos
    var selectionStartChar by mutableStateOf<PdfChar?>(null)
    var selectionEndChar by mutableStateOf<PdfChar?>(null)

    // Para saber si ocultar el menú flotante mientras se arrastra
    var isDraggingHandle by mutableStateOf(false)

    private val pageModels = mutableMapOf<Int, PageModel>()
    private val optimizedPageModels = mutableMapOf<Int, com.fmaestre98.pdfviewer.pdfViewer.model.OptimizedPageModel>()

    // Nuevo método para obtener/setear PageModel por página
    fun getPageModel(pageIndex: Int): PageModel? = pageModels[pageIndex]
    
    fun getOptimizedPageModel(pageIndex: Int): com.fmaestre98.pdfviewer.pdfViewer.model.OptimizedPageModel? {
        val model = pageModels[pageIndex] ?: return null
        return optimizedPageModels.getOrPut(pageIndex) { 
            com.fmaestre98.pdfviewer.pdfViewer.model.OptimizedPageModel(model) 
        }
    }

    fun setPageModel(pageIndex: Int, pageModel: PageModel?, currentPageIndex: Int = -1) {
        if (pageModel == null) {
            pageModels.remove(pageIndex)
            optimizedPageModels.remove(pageIndex)
        } else {
            pageModels[pageIndex] = pageModel
            // Lazy creation of optimized model when requested, or clear old one
            optimizedPageModels.remove(pageIndex) 
            
            // Clean up distant pages when map reaches 20 elements
            if (pageModels.size >= 20) {
                val referencePage = if (currentPageIndex >= 0) currentPageIndex else pageIndex
                cleanupDistantPages(referencePage)
            }
        }
    }
    
    /**
     * Removes half of the pages that are farthest from the reference page.
     * Keeps pages closer to the reference page.
     */
    private fun cleanupDistantPages(referencePage: Int) {
        if (pageModels.size < 20) return
        
        val pagesToRemove = pageModels.size / 2
        val sortedByDistance = pageModels.keys.sortedBy { kotlin.math.abs(it - referencePage) }
        
        // Remove the farthest pages (keep the closest ones)
        sortedByDistance.takeLast(pagesToRemove).forEach { pageIndex ->
            pageModels.remove(pageIndex)
            optimizedPageModels.remove(pageIndex)
        }
    }

    var currentReadingWord by mutableStateOf<PdfWord?>(null)
    var isKaraokeMode by mutableStateOf(false)
    var isTtsAvailable by mutableStateOf(false)

    var selectionPageIndex by mutableIntStateOf(-1)

    // Persistent highlights data (page index -> list of highlight rects)
    private var highlightsMap by mutableStateOf<Map<Int, List<HighlightData>>>(emptyMap())

    fun setHighlights(highlights: Map<Int, List<HighlightData>>) {
        highlightsMap = highlights
    }

    fun getHighlightsForPage(pageIndex: Int): List<HighlightData> {
        return highlightsMap[pageIndex] ?: emptyList()
    }

    fun addHighlightsForPage(pageIndex: Int, newHighlights: List<HighlightData>) {
        val current = highlightsMap.toMutableMap()
        val existing = current[pageIndex]?.toMutableList() ?: mutableListOf()
        existing.addAll(newHighlights)
        current[pageIndex] = existing
        highlightsMap = current
    }

    fun setKaraokeModeExtra(enabled: Boolean) {
        isKaraokeMode = enabled
        if (!enabled) {
            currentReadingWord = null
        }
    }

    fun setTtsAvailableExtra(available: Boolean) {
        isTtsAvailable = available
    }

    // CAMBIO: Lógica para obtener el texto plano seleccionado
    fun getSelectedText(): String {
        val start = selectionStartChar ?: return ""
        val end = selectionEndChar ?: return ""
        val model = pageModels[selectionPageIndex] ?: return ""

        // Lógica simplificada: Iterar sobre palabras y extraer caracteres en rango
        val sb = StringBuilder()
        var recording = false

        // Esta es una implementación lineal simple.
        // Para mayor eficiencia, se podrían buscar índices, pero esto funciona bien para páginas normales.
        model.coordinates.forEach { line ->
            line.words.forEach { word ->
                word.characters.forEach { char ->
                    if (char == start) recording = true
                    if (recording) sb.append(char.text)
                    if (char == end) {
                        recording = false
                        return sb.toString()
                    }
                }
                // Agregar espacio entre palabras si estamos grabando y no es el final
                if (recording) sb.append(" ")
            }
            if (recording) sb.append("\n")
        }
        return sb.toString()
    }

    fun activateTextSelection(word: PdfWord, pageIndex: Int) {
        selectionStartChar = word.characters.firstOrNull()
        selectionEndChar = word.characters.lastOrNull()
        selectionPageIndex = pageIndex
        isDraggingHandle = false
    }

    fun deactivateTextSelection() {
        selectionStartChar = null
        selectionEndChar = null
        selectionPageIndex = -1 // Reset
        isDraggingHandle = false
    }

    fun updateSelectionStart(char: PdfChar) {
        selectionStartChar = char
    }

    fun updateSelectionEnd(char: PdfChar) {
        selectionEndChar = char
    }

    /**
     * Resets zoom and pan to default values.
     */
    fun resetZoom() {
        zoomLevel = 1.0f
        offsetX = 0f
        offsetY = 0f
    }

    /**
     * Clears all interactive state.
     */
    fun reset() {
        zoomLevel = 1.0f
        offsetX = 0f
        offsetY = 0f
        selectionStartChar = null
        selectionEndChar = null
        pageModels.clear()
        optimizedPageModels.clear()
        isDraggingHandle = false
        highlightsMap = emptyMap()
    }
}

