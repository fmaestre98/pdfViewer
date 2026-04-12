package com.fmaestre98.pdfviewer.pdfViewer.utils

import androidx.compose.ui.geometry.Offset
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfChar
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfLine
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord

/**
 * Helper functions for PDF coordinate conversion and word finding.
 * Extracted from GestureHandler for reuse.
 */
object PdfCoordinateHelpers {
    /**
     * Converts screen coordinates to PDF page coordinates.
     */
    fun convertToPageCoordinates(
        tapOffset: Offset,
        pageWidth: Int,
        pageHeight: Int,
        screenWidth: Float,
        screenHeight: Float,
        zoomLevel: Float,
        offsetX: Float,
        offsetY: Float
    ): Pair<Float, Float> {
        // 1. Obtener dimensiones del contenedor (pantalla disponible)
        val viewWidth = screenWidth
        val viewHeight = screenHeight

        // 2. Calcular las dimensiones visuales de la página renderizada (Fit center)
        // Esto replica lo que hace ContentScale.Fit
        val scaleX = viewWidth / pageWidth
        val scaleY = viewHeight / pageHeight
        val baseScale = minOf(scaleX, scaleY) // Escala base para que quepa (Fit)

        val renderedWidth = pageWidth * baseScale
        val renderedHeight = pageHeight * baseScale

        // 3. Calcular el desplazamiento causado por el centrado (Letterboxing)
        val centeringOffsetX = (viewWidth - renderedWidth) / 2f
        val centeringOffsetY = (viewHeight - renderedHeight) / 2f

        // 4. Transformación Inversa completa:
        // (Tap - Pan - Centrado) / Zoom / EscalaBase

        // Paso A: Quitar el Pan y Zoom del gesto del usuario
        // Nota: El zoom del usuario se aplica sobre el centro de la pantalla.
        // La fórmula simplificada asumiendo que el pivote es el centro:
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        val unzoomedX = (tapOffset.x - offsetX - centerX) / zoomLevel + centerX
        val unzoomedY = (tapOffset.y - offsetY - centerY) / zoomLevel + centerY

        // Paso B: Quitar el offset del centrado (Fit) y la escala base
        val pdfX = (unzoomedX - centeringOffsetX) / baseScale
        val pdfY = (unzoomedY - centeringOffsetY) / baseScale

        println("Conversion: Tap=$tapOffset -> PDF=($pdfX, $pdfY)")

        return pdfX to pdfY
    }

    /**
     * Converts PDF page coordinates to screen coordinates.
     * This is the inverse of convertToPageCoordinates, used for overlay positioning.
     */
    fun convertToScreenCoordinates(
        pdfPoint: Offset,
        pageWidth: Int,
        pageHeight: Int,
        screenWidth: Float,
        screenHeight: Float,
        zoomLevel: Float,
        offsetX: Float,
        offsetY: Float
    ): Offset {
        // 1. Calc base scale (Fit Center)
        val viewWidth = screenWidth
        val viewHeight = screenHeight

        val scaleX = viewWidth / pageWidth
        val scaleY = viewHeight / pageHeight
        val baseScale = minOf(scaleX, scaleY)

        val renderedWidth = pageWidth * baseScale
        val renderedHeight = pageHeight * baseScale

        val centeringOffsetX = (viewWidth - renderedWidth) / 2f
        val centeringOffsetY = (viewHeight - renderedHeight) / 2f

        // 2. Apply Base Scale & Centering to get "Unzoomed Viewport Coordinates"
        val unzoomedX = pdfPoint.x * baseScale + centeringOffsetX
        val unzoomedY = pdfPoint.y * baseScale + centeringOffsetY

        // 3. Apply Zoom & Pan (Pivot is Center)
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        // Formula: Scaled = (Unscaled - Pivot) * Zoom + Pivot
        val zoomedX = (unzoomedX - centerX) * zoomLevel + centerX
        val zoomedY = (unzoomedY - centerY) * zoomLevel + centerY

        // 4. Apply Pan
        val finalX = zoomedX + offsetX
        val finalY = zoomedY + offsetY

        return Offset(finalX, finalY)
    }

    /**
     * Convierte coordenadas de pantalla a PDF específicamente para LazyColumn (Vertical).
     * @param tapY La posición Y del toque relativa al borde superior del ITEM de la lista (no de la pantalla).
     */
    fun convertToPageCoordinatesVertical(
        tapX: Float,
        tapY: Float, // Coordenada Y LOCAL dentro del item de la lista
        pageWidth: Int,
        pageHeight: Int,
        viewWidth: Float // Ancho de la vista disponible (ancho de pantalla)
    ): Pair<Float, Float> {
        // En vertical, asumimos Fit Width (la página llena el ancho)
        // Calculamos la escala basada en el ancho
        val scale = viewWidth / pageWidth

        // Calculamos el offset X si la página es más estrecha que la pantalla (raro en vertical móvil, pero posible)
        val renderedWidth = pageWidth * scale
        val centeringOffsetX = (viewWidth - renderedWidth) / 2f

        // Transformación inversa
        // X: Restamos el padding lateral y dividimos por la escala
        val pdfX = (tapX - centeringOffsetX) / scale

        // Y: En vertical dentro de LazyColumn, el tapY ya es relativo al inicio de la página renderizada.
        // Solo necesitamos dividir por la escala.
        val pdfY = tapY / scale

        return pdfX to pdfY
    }

    /**
     * Finds a word at the given coordinates in the page model.
     */
    fun findWordInModel(
        pageModel: PageModel,
        x: Float,
        y: Float
    ): PdfWord? {
        // Padding de tolerancia para facilitar el toque (en píxeles del PDF)
        val hitboxPadding = 5f
        
        println("my-logs findWordInModel: searching for point ($x, $y)")
        var wordCount = 0
        pageModel.coordinates.forEach { line ->
            line.words.forEach { word ->
                wordCount++
                // Crear un rectángulo expandido con padding para mayor tolerancia
                val expandedRect = android.graphics.RectF(
                    word.rect.left - hitboxPadding,
                    word.rect.top - hitboxPadding,
                    word.rect.right + hitboxPadding,
                    word.rect.bottom + hitboxPadding
                )
                val contains = expandedRect.contains(x, y)

                println("  Word '${word.text}' at rect ${word.rect} (expanded: $expandedRect) contains=$contains")

                if (contains) {
                    println("my-logs  FOUND: Word '${word.text}' at rect ${word.rect}")
                    return word
                }
            }
        }
        println("my-logs  No word found. Checked $wordCount words total.")
        return null
    }

    /**
     * Converts PDF page coordinates to screen coordinates for Vertical View (LazyColumn).
     */
    fun convertToScreenCoordinatesVertical(
        pdfPoint: Offset,
        pageWidth: Int,
        pageHeight: Int,
        itemOffset: Int, // Y offset of the item in the LazyColumn
        screenWidth: Float,
        screenHeight: Float,
        zoomLevel: Float,
        offsetX: Float
    ): Offset {
        // 1. Calculate Scale & Centering (same as convertToPageCoordinatesVertical)
        // In vertical mode, pages fit width (usually)
        val scale = screenWidth / pageWidth
        
        // Handle centering if page is narrower than screen
        val renderedWidth = pageWidth * scale
        val centeringOffsetX = (screenWidth - renderedWidth) / 2f
        
        // 2. Calculate "Layout Coordinates" (position inside Un-zoomed LazyColumn)
        val layoutX = pdfPoint.x * scale + centeringOffsetX
        val layoutY = itemOffset + pdfPoint.y * scale
        
        // 3. Apply Global Zoom & Pan (Pivot is Center of Screen)
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        
        // Formula: Scaled = (Unscaled - Pivot) * Zoom + Pivot + Pan
        val screenX = (layoutX - centerX) * zoomLevel + centerX + offsetX
        val screenY = (layoutY - centerY) * zoomLevel + centerY // panY is 0 in vertical view
        
        return Offset(screenX, screenY)
    }

    /**
     * Nuevo método para encontrar el carácter más cercano.
     * Primero busca la palabra, luego el carácter dentro de ella.
     */
    /**
     * Nuevo método para encontrar el carácter más cercano.
     * Primero busca la palabra, luego el carácter dentro de ella.
     */
    fun findCharInModel(pageModel: PageModel, x: Float, y: Float): PdfChar? {
        println("my-logs findCharInModel(PageModel) x=$x y=$y")
        // Implementation remains same for non-optimized calls
        val searchRadius = 30f
        var closestChar: PdfChar? = null
        var minDistanceSq = searchRadius * searchRadius

        // Pre-calc the line with minimal vertical distance — this limits checks quickly
        val nearestLine = pageModel.coordinates.minByOrNull { line -> kotlin.math.abs(y - line.rect.centerY()) }
        if (nearestLine == null) return null

        // Check only nearestLine and its immediate neighbours (safety)
        val linesToSearch = pageModel.coordinates.filter { line ->
            kotlin.math.abs(line.rect.centerY() - nearestLine.rect.centerY()) <= (nearestLine.rect.height() + 40f)
        }

        linesToSearch.forEach { line ->
            // quick vertical reject
            if (y < line.rect.top - 20f || y > line.rect.bottom + 20f) return@forEach
            line.words.forEach { word ->
                // quick horizontal reject
                if (x < word.rect.left - 60f || x > word.rect.right + 60f) return@forEach
                word.characters.forEach { char ->
                    val rect = char.rect
                    if (rect.contains(x, y)) return char
                    val distance = minOf(
                        kotlin.math.abs(x - rect.left),
                        kotlin.math.abs(x - rect.right),
                        kotlin.math.abs(y - rect.top),
                        kotlin.math.abs(y - rect.bottom)
                    )
                    val distSq = distance * distance
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        closestChar = char
                    }
                }
            }
        }
        return closestChar
    }
    
    /**
     * Finds character using the optimized spatial grid.
     */
    fun findCharInModel(
        optimizedModel: com.fmaestre98.pdfviewer.pdfViewer.model.OptimizedPageModel, 
        x: Float, 
        y: Float
    ): PdfChar? {
        // Use tolerance of 30f to match the original implementation's searchRadius
        return optimizedModel.findCharNear(x, y, 30f)
    }


    private fun findCharInLine(line: PdfLine, x: Float, y: Float): PdfChar? {
        var closestChar: PdfChar? = null
        var minDistance = Float.MAX_VALUE

        line.words.forEach { word ->
            word.characters.forEach { char ->
                val rect = char.rect
                val distance = Math.abs(x - rect.centerX())

                if (distance < minDistance) {
                    minDistance = distance
                    closestChar = char
                }
            }
        }

        return closestChar
    }
}

