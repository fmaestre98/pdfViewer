package com.fmaestre98.pdfviewer.pdfViewer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fmaestre98.pdfviewer.pdfViewer.model.HighlightData
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfChar
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord

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

    var pendingSearchQuery by mutableStateOf<String?>(null)
    var pendingSearchPage by mutableIntStateOf(-1)

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

    var selectionStartPageIndex by mutableIntStateOf(-1)
    var selectionEndPageIndex by mutableIntStateOf(-1)

    var selectionPageIndex: Int
        get() = if (selectionStartPageIndex != -1) selectionStartPageIndex else -1
        set(value) {
            selectionStartPageIndex = value
            selectionEndPageIndex = value
        }

    val isSelectionActiveOnPage: (Int) -> Boolean = { pageIndex ->
        if (selectionStartPageIndex == -1 || selectionEndPageIndex == -1) false
        else {
            val minP = kotlin.math.min(selectionStartPageIndex, selectionEndPageIndex)
            val maxP = kotlin.math.max(selectionStartPageIndex, selectionEndPageIndex)
            pageIndex in minP..maxP
        }
    }

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

    private fun isCharBeforeOrEqual(a: PdfChar, b: PdfChar, model: PageModel): Boolean {
        if (a === b || a.id == b.id) return true
        model.coordinates.forEach { line ->
            line.words.forEach { word ->
                word.characters.forEach { char ->
                    if (char === a || char.id == a.id) return true
                    if (char === b || char.id == b.id) return false
                }
            }
        }
        return true
    }

    fun getSelectedText(): String {
        val startChar = selectionStartChar ?: return ""
        val endChar = selectionEndChar ?: return ""
        val startPage = selectionStartPageIndex
        val endPage = selectionEndPageIndex
        if (startPage < 0 || endPage < 0) return ""

        val sb = StringBuilder()

        if (startPage == endPage) {
            val model = pageModels[startPage] ?: return ""
            val isStartFirst = isCharBeforeOrEqual(startChar, endChar, model)
            val first = if (isStartFirst) startChar else endChar
            val last = if (isStartFirst) endChar else startChar

            var recording = false
            model.coordinates.forEach { line ->
                line.words.forEach { word ->
                    word.characters.forEach { char ->
                        if (char === first || char.id == first.id) recording = true
                        if (recording) sb.append(char.text)
                        if (char === last || char.id == last.id) {
                            recording = false
                            return sb.toString()
                        }
                    }
                    if (recording) sb.append(" ")
                }
                if (recording) sb.append("\n")
            }
            return sb.toString()
        }

        // Multi-page extraction
        val isForward = startPage < endPage
        val actualStartPage = if (isForward) startPage else endPage
        val actualEndPage = if (isForward) endPage else startPage
        val actualStartChar = if (isForward) startChar else endChar
        val actualEndChar = if (isForward) endChar else startChar

        for (p in actualStartPage..actualEndPage) {
            val model = pageModels[p] ?: continue
            var recording = (p > actualStartPage)

            model.coordinates.forEach { line ->
                line.words.forEach { word ->
                    word.characters.forEach { char ->
                        if (p == actualStartPage && (char === actualStartChar || char.id == actualStartChar.id)) recording = true
                        if (recording) sb.append(char.text)
                        if (p == actualEndPage && (char === actualEndChar || char.id == actualEndChar.id)) {
                            recording = false
                        }
                    }
                    if (recording) sb.append(" ")
                }
                if (recording) sb.append("\n")
            }
            if (p < actualEndPage && sb.isNotEmpty()) {
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    fun activateTextSelection(word: PdfWord, pageIndex: Int) {
        selectionStartChar = word.characters.firstOrNull()
        selectionEndChar = word.characters.lastOrNull()
        selectionStartPageIndex = pageIndex
        selectionEndPageIndex = pageIndex
        isDraggingHandle = false
    }

    fun deactivateTextSelection() {
        selectionStartChar = null
        selectionEndChar = null
        selectionStartPageIndex = -1
        selectionEndPageIndex = -1
        isDraggingHandle = false
    }

    fun isPositionBeforeOrEqual(pageA: Int, charA: PdfChar, pageB: Int, charB: PdfChar): Boolean {
        if (pageA < pageB) return true
        if (pageA > pageB) return false
        val model = pageModels[pageA] ?: return true
        return isCharBeforeOrEqual(charA, charB, model)
    }

    fun updateSelectionStart(char: PdfChar, pageIndex: Int = selectionStartPageIndex) {
        val currentEndChar = selectionEndChar
        val currentEndPage = selectionEndPageIndex
        val targetPage = if (pageIndex >= 0) pageIndex else selectionStartPageIndex

        if (currentEndChar != null && currentEndPage >= 0 && !isPositionBeforeOrEqual(targetPage, char, currentEndPage, currentEndChar)) {
            // Crossed: start handle dragged past end handle. Swap!
            selectionStartChar = currentEndChar
            selectionStartPageIndex = currentEndPage
            selectionEndChar = char
            selectionEndPageIndex = targetPage
        } else {
            selectionStartChar = char
            if (targetPage >= 0) selectionStartPageIndex = targetPage
        }
    }

    fun updateSelectionEnd(char: PdfChar, pageIndex: Int = selectionEndPageIndex) {
        val currentStartChar = selectionStartChar
        val currentStartPage = selectionStartPageIndex
        val targetPage = if (pageIndex >= 0) pageIndex else selectionEndPageIndex

        if (currentStartChar != null && currentStartPage >= 0 && !isPositionBeforeOrEqual(currentStartPage, currentStartChar, targetPage, char)) {
            // Crossed: end handle dragged before start handle. Swap!
            selectionEndChar = currentStartChar
            selectionEndPageIndex = currentStartPage
            selectionStartChar = char
            selectionStartPageIndex = targetPage
        } else {
            selectionEndChar = char
            if (targetPage >= 0) selectionEndPageIndex = targetPage
        }
    }

    /**
     * Busca el texto proporcionado dentro de la página indicada y, si lo encuentra,
     * establece la selección de texto sobre él.
     */
    fun selectTextByQuery(query: String, pageIndex: Int) {
        val model = pageModels[pageIndex] ?: return
        if (query.isBlank()) return
        
        val queryLower = query.lowercase().replace(Regex("\\s+"), "")

        val allChars = mutableListOf<PdfChar>()
        model.coordinates.forEach { line ->
            line.words.forEach { word ->
                allChars.addAll(word.characters)
            }
        }
        
        val fullText = allChars.joinToString("") { it.text }.lowercase()
        val matchIndex = fullText.indexOf(queryLower)
        
        if (matchIndex != -1) {
            val startChar = allChars[matchIndex]
            val endChar = allChars[minOf(allChars.size - 1, matchIndex + queryLower.length - 1)]
            selectionStartChar = startChar
            selectionEndChar = endChar
            selectionPageIndex = pageIndex
            isDraggingHandle = false
        }
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

