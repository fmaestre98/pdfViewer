package com.fmaestre98.pdfviewer.pdfViewer.rendering

import android.graphics.Bitmap
import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.*
import com.fmaestre98.pdfviewer.pdfViewer.text.PdfTextExtractor
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class PdfPageRenderer(
    private val rendererManager: PdfRendererManager,
    private val context: Context,
    private val pdfFile: java.io.File,
    private val maxCacheSize: Int = 5,
    private val enableTextExtraction: Boolean = false
) {

    private val cache = ConcurrentHashMap<String, RenderedPage>()
    private val textModelCache = ConcurrentHashMap<Int, PageModel>()
    private val renderQueue = mutableSetOf<Int>()
    private val isDisposed = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Lazy initialization del extractor de texto
    private val textExtractor: PdfTextExtractor? by lazy {
        if (enableTextExtraction) {
            try {
                runBlocking { // Solo para inicialización
                    val pageSize = rendererManager.getPageSize(0) // Tamaño de primera página
                    if (pageSize != null) {
                        PdfTextExtractor.create(context, pdfFile, pageSize)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Renderiza página completa (solo bitmap, sin texto).
     * La extracción de texto se hace por separado para mejor organización.
     */
    suspend fun renderPage(
        pageIndex: Int,
        scaleFactor: Float = 1.0f,
        maxWidth: Int = 0,
        maxHeight: Int = 0
    ): RenderedPage? = withContext(Dispatchers.Default) {
        if (isDisposed.get()) return@withContext null

        val cacheKey = generateCacheKey(pageIndex, scaleFactor, maxWidth, maxHeight, false)

        // Verificar caché
        cache[cacheKey]?.let { return@withContext it }

        if (renderQueue.contains(pageIndex)) {
            delay(50)
            cache[cacheKey]?.let { return@withContext it }
        }

        renderQueue.add(pageIndex)
        try {
            // Evitamos renderizar si la corrutina ya fue cancelada (ej. scroll rápido)
            ensureActive()

            // Renderizar bitmap
            val bitmap = rendererManager.renderPage(pageIndex, scaleFactor, maxWidth, maxHeight)
            
            // Volvemos a comprobar la cancelación antes de cachear
            if (bitmap == null || isDisposed.get() || !isActive) return@withContext null

            // Crear RenderedPage (sin texto)
            val renderedPage = RenderedPage(
                pageIndex = pageIndex,
                bitmap = bitmap,
                width = bitmap.width,
                height = bitmap.height,
                scaleFactor = scaleFactor,
            )

            // Almacenar en caché
            if (cache.size >= maxCacheSize) {
                evictOldestCacheEntry()
            }
            cache[cacheKey] = renderedPage

            renderedPage
        } catch (e: Exception) {
            if (isDisposed.get()) null else throw e
        } finally {
            renderQueue.remove(pageIndex)
        }
    }

    /**
     * Obtiene el PageModel de una página (con caché).
     * Extrae el texto si no está en caché y ajusta las coordenadas al tamaño objetivo.
     * 
     * Nota: El método adjustCoordinatesToScreenSize modifica el modelo original,
     * por lo que siempre extraemos desde el modelo original en caché y ajustamos.
     */
    suspend fun getPageText(pageIndex: Int, targetWidth: Int, targetHeight: Int): PageModel? =
        withContext(Dispatchers.IO) {
            println("my-logs getPageText: pageIndex=$pageIndex, targetWidth=$targetWidth, targetHeight=$targetHeight")
            if (!enableTextExtraction || textExtractor == null) {
                return@withContext null
            }

            try {
                // Verificar caché primero
                var model = textModelCache[pageIndex]
                
                if (model == null) {
                    // Extraer texto si no está en caché
                    val pageSize = rendererManager.getPageSize(pageIndex)
                    if (pageSize == null) return@withContext null

                    model = textExtractor!!.extractPageModel(pageIndex)
                    if (model == null) return@withContext null
                    
                    // Almacenar en caché (con coordenadas originales)
                    textModelCache[pageIndex] = model
                }

                // Crear una nueva instancia de PageModel con nuevas listas para evitar modificar el caché
                val adjustedModel = PageModel(
                    coordinates = ArrayList(model.coordinates),
                    relativeSizeCalculated = false,
                    targetWidth = targetWidth,
                    targetHeight = targetHeight,
                )
                
                // Ajustar coordenadas al tamaño objetivo
                return@withContext textExtractor!!.adjustCoordinatesToScreenSize(
                    adjustedModel,
                    targetWidth.toFloat(),
                    targetHeight.toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Generates a cache key based on page index and render parameters.
     */
    private fun generateCacheKey(
        pageIndex: Int,
        scaleFactor: Float,
        maxWidth: Int,
        maxHeight: Int,
        includeText: Boolean = false
    ): String {
        return "${pageIndex}_${scaleFactor}_${maxWidth}_${maxHeight}_${includeText}"
    }

    /**
     * Limpia el caché de modelos de texto.
     */
    fun clearTextModelCache() {
        textModelCache.clear()
    }

    /**
     * Limpia el modelo de texto de una página específica.
     */
    fun clearPageTextModel(pageIndex: Int) {
        textModelCache.remove(pageIndex)
    }

    /**
     * Cleans up resources.
     * Cancels all pending operations before disposing.
     */
    fun dispose() {
        isDisposed.set(true)
        scope.cancel()

        // Cerrar extractor de texto si existe
        textExtractor?.close()

        // Limpiar cachés (no reciclar bitmaps que puedan estar en uso)
        cache.clear()
        textModelCache.clear()
        renderQueue.clear()
    }

    /**
     * Gets page size without rendering.
     */
    suspend fun getPageSize(pageIndex: Int): Pair<Int, Int>? = withContext(Dispatchers.Default) {
        if (isDisposed.get() || !isActive) {
            return@withContext null
        }
        
        try {
            rendererManager.getPageSize(pageIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isDisposed.get()) {
                null
            } else {
                throw e
            }
        }
    }
    
    /**
     * Clears the cache.
     */
    fun clearCache() {
        // Don't recycle bitmaps - they may still be in use by Compose
        // The GC will handle memory management
        cache.clear()
    }
    
    /**
     * Removes a specific page from cache.
     */
    fun removeFromCache(pageIndex: Int) {
        val keysToRemove = cache.keys.filter { it.startsWith("${pageIndex}_") }
        keysToRemove.forEach { key ->
            // Don't recycle bitmaps - they may still be in use by Compose
            cache.remove(key)
        }
    }
    
    /**
     * Evicts the oldest cache entry (simple FIFO).
     */
    private fun evictOldestCacheEntry() {
        if (cache.isNotEmpty()) {
            val firstKey = cache.keys.first()
            // Don't recycle bitmaps - they may still be in use by Compose
            cache.remove(firstKey)
        }
    }
}

/**
 * Data class representing a rendered page with its metadata.
 */
@Immutable
data class RenderedPage(
    val pageIndex: Int,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val scaleFactor: Float,
)

