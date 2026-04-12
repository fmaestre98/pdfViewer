package com.fmaestre98.pdfviewer.pdfViewer.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import com.fmaestre98.pdfviewer.pdfViewer.text.TtsManager
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfPageRenderer
import com.fmaestre98.pdfviewer.pdfViewer.rendering.PdfRendererManager
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for managing PDF viewer business logic.
 * Handles PDF loading, text extraction, page model loading, and TTS.
 */
@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val ttsManager: TtsManager
) : ViewModel() {
    var loaderState by mutableStateOf(PdfLoaderState())
        private set

    var navigationState by mutableStateOf(PdfNavigationState())
        private set

    var interactiveState by mutableStateOf(PdfInteractiveState())
        private set

    private var pageRenderer: PdfPageRenderer? = null

    init {
        // Configurar callback para resaltar palabras
        ttsManager.onWordHighlight = { word ->
            interactiveState.currentReadingWord = word
        }

        // Configurar callback para errores de inicialización
        ttsManager.onInitializationError = { error ->
            interactiveState.setTtsAvailableExtra(false)
        }

        ttsManager.onInitializationSuccess = {
            interactiveState.setTtsAvailableExtra(true)
        }
    }

    /**
     * Loads a PDF file and initializes the renderer.
     */
    fun loadPdf(pdfFile: File, context: Context, enableTextExtraction: Boolean) {
        viewModelScope.launch {
            try {
                loaderState.setLoadingExtra(true)
                val rendererManager = withContext(Dispatchers.IO) {
                    PdfRendererManager.create(pdfFile)
                }
                loaderState.setRendererManagerExtra(rendererManager)

                // Create PdfPageRenderer with text extraction if enabled
                pageRenderer = PdfPageRenderer(
                    rendererManager = rendererManager,
                    context = context,
                    pdfFile = pdfFile,
                    enableTextExtraction = enableTextExtraction
                )
            } catch (e: Exception) {
                loaderState.setErrorExtra(e)
                throw e
            }
        }
    }

    /**
     * Loads the text model for a specific page.
     */
    fun loadPageTextModel(pageIndex: Int, targetWidth: Int, targetHeight: Int) {
        viewModelScope.launch {
            val renderer = pageRenderer ?: return@launch
            val loadedPageModel = interactiveState.getPageModel(pageIndex)
            if (loadedPageModel != null && loadedPageModel.targetHeight == targetHeight && loadedPageModel.targetWidth == targetWidth) {
                // Ya está cargado
                return@launch
            }
            val pageModel = renderer.getPageText(pageIndex, targetWidth, targetHeight)
            // Pass current page index for cleanup reference
            interactiveState.setPageModel(pageIndex, pageModel, navigationState.currentPageIndex)
            interactiveState.deactivateTextSelection()

            // Si el modo karaoke está activo y es la página actual, hablar automáticamente
            if (interactiveState.isKaraokeMode &&
                navigationState.currentPageIndex == pageIndex &&
                pageModel != null &&
                interactiveState.isTtsAvailable
            ) {
                ttsManager.speakPage(pageModel)
            }
        }
    }

    /**
     * Sets the current page index and optionally loads the text model.
     */
    fun setCurrentPage(
        pageIndex: Int,
        optimalSize: Pair<Int, Int>? = null,
        verticalView: Boolean = false
    ) {
        println("my-logs called setCurrentPage pageIndex=$pageIndex optimalSize=$optimalSize")
        // Desactivar selección de texto si cambia la página
        if (navigationState.currentPageIndex != pageIndex && interactiveState.isTextSelectionActive) {
            // Solo desactivar si la selección está en una página diferente
            if (interactiveState.selectionPageIndex != pageIndex) {
                interactiveState.deactivateTextSelection()
            }
        }
        
        // Detener TTS si está activo al cambiar de página
        if (interactiveState.isKaraokeMode && navigationState.currentPageIndex != pageIndex) {
            ttsManager.stop()
            interactiveState.currentReadingWord = null
        }

        navigationState.setCurrentPage(pageIndex)
        if (verticalView) {
            interactiveState.resetZoom()
        }

        // Load text model if optimal size is provided
        optimalSize?.let { (width, height) ->
            loadPageTextModel(pageIndex, width, height)
        }
    }

    /**
     * Gets the current page renderer.
     */
    fun getPageRenderer(): PdfPageRenderer? = pageRenderer

    /**
     * Verifica la disponibilidad de TTS.
     */
    fun checkTtsAvailability() {
        val available = ttsManager.isAvailable()
        interactiveState.setTtsAvailableExtra(available)
    }

    /**
     * Alterna el modo karaoke.
     */
    fun toggleKaraokeMode() {
        val newMode = !interactiveState.isKaraokeMode
        interactiveState.setKaraokeModeExtra(newMode)

        if (newMode) {
            // Iniciar modo karaoke
            checkTtsAvailability()
            if (interactiveState.isTtsAvailable) {
                // Hablar la página actual si el modelo ya está cargado
                val pageModel = interactiveState.getPageModel(navigationState.currentPageIndex)
                if (pageModel != null) {
                    ttsManager.speakPage(pageModel)
                }
            }
        } else {
            // Detener modo karaoke
            ttsManager.stop()
            interactiveState.currentReadingWord = null
        }
    }

    /**
     * Habla una página específica.
     */
    fun speakPage(pageModel: PageModel) {
        if (interactiveState.isKaraokeMode && interactiveState.isTtsAvailable) {
            ttsManager.speakPage(pageModel)
        }
    }

    /**
     * Detiene TTS.
     */
    fun stopTts() {
        ttsManager.stop()
        interactiveState.currentReadingWord = null
        interactiveState.setKaraokeModeExtra(false)
    }

    /**
     * Cleans up resources.
     */
    override fun onCleared() {
        super.onCleared()
        // No destruimos el TtsManager aquí porque es un singleton
        // Se destruirá cuando la aplicación se cierre
        ttsManager.stop()
        pageRenderer?.dispose()
        pageRenderer = null
        loaderState.dispose()
        navigationState.reset()
        interactiveState.reset()
    }
}

