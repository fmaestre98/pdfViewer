package com.fmaestre98.pdfviewer.pdfViewer.model

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Wrapper for PageModel that pre-processes data into a spatial grid for O(1) lookup.
 * This significantly improves performance for character finding during drag operations.
 */
class OptimizedPageModel(val pageModel: PageModel) {
    // Grid spatial para búsqueda O(1)
    private val charGrid: Array<Array<MutableList<PdfChar>>> by lazy {
        createSpatialGrid()
    }
    
    // Cache page dimensions
    private val width: Float
    private val height: Float
    
    init {
        // Find max dimensions from content if not explicitly available
        var maxX = 0f
        var maxY = 0f
        
        pageModel.coordinates.forEach { line ->
            maxX = maxOf(maxX, line.rect.right)
            maxY = maxOf(maxY, line.rect.bottom)
        }
        
        // Add some padding to ensure everything fits
        width = maxOf(maxX, 1f)
        height = maxOf(maxY, 1f)
    }

    private fun createSpatialGrid(): Array<Array<MutableList<PdfChar>>> {
        // Dividir página en grid 10x10
        val gridSize = 10
        val grid = Array(gridSize) { Array(gridSize) { mutableListOf<PdfChar>() } }
        
        pageModel.coordinates.forEach { line ->
            line.words.forEach { word ->
                word.characters.forEach { char ->
                    // Calculate grid position based on center of character
                    val centerX = char.rect.centerX()
                    val centerY = char.rect.centerY()
                    
                    val gridX = (centerX / width * gridSize).toInt().coerceIn(0, gridSize - 1)
                    val gridY = (centerY / height * gridSize).toInt().coerceIn(0, gridSize - 1)
                    
                    grid[gridY][gridX].add(char)
                }
            }
        }
        return grid
    }
    
    /**
     * Finds the character nearest to the given coordinates using the spatial grid.
     * Search is limited to the target cell and its immediate neighbors (3x3 area).
     */
    fun findCharNear(x: Float, y: Float, tolerance: Float = 40f): PdfChar? {
        val gridSize = 10
        val gridX = (x / width * gridSize).toInt().coerceIn(0, gridSize - 1)
        val gridY = (y / height * gridSize).toInt().coerceIn(0, gridSize - 1)
        
        var closestChar: PdfChar? = null
        var minDistanceSq = tolerance * tolerance
        
        // Buscar en celda y celdas adyacentes (3x3)
        for (dy in -1..1) {
            for (dx in -1..1) {
                val cellX = gridX + dx
                val cellY = gridY + dy
                
                if (cellX in 0 until gridSize && cellY in 0 until gridSize) {
                    val cellChars = charGrid[cellY][cellX]
                    
                    for (char in cellChars) {
                        val rect = char.rect
                        if (rect.contains(x, y)) return char
                        
                        // Calculate distance to center
                        val distSq = rect.centerDistanceSq(x, y)
                        
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq
                            closestChar = char
                        }
                    }
                }
            }
        }
        return closestChar
    }
    
    /**
     * Efficiently retrieves all characters between start and end chars.
     * Sorts characters by position (Y major, X minor) to ensure correct reading order.
     */
    fun getCharactersBetween(startChar: PdfChar, endChar: PdfChar): List<PdfChar> {
        // Implementar como en Views: recorrer líneas y extraer caracteres entre puntos
        val allChars = mutableListOf<PdfChar>()
        
        // We can just flatten the page model structure since it's already sorted by line/word order
        // which is usually correct reading order
        pageModel.coordinates.forEach { line ->
            line.words.forEach { word ->
                allChars.addAll(word.characters)
            }
        }
        
        // Find indices of start and end chars
        // Using object identity if possible, or ID fallback
        val startIndex = allChars.indexOfFirst { it.id == startChar.id || it === startChar }
        val endIndex = allChars.indexOfFirst { it.id == endChar.id || it === endChar }
        
        if (startIndex == -1 || endIndex == -1) return emptyList()
        
        val (min, max) = if (startIndex < endIndex) startIndex to endIndex else endIndex to startIndex
        return allChars.subList(min, max + 1)
    }
    
    // Helper extension for distance
    private fun RectF.centerDistanceSq(x: Float, y: Float): Float {
        val dx = centerX() - x
        val dy = centerY() - y
        return dx * dx + dy * dy
    }
}
