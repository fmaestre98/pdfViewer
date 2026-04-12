package com.fmaestre98.pdfviewer.pdfViewer.rendering

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Manages the lifecycle of PdfRenderer and handles page rendering.
 * Uses Android's native PdfRenderer API (API 21+).
 */
class PdfRendererManager private constructor(
    private val fileDescriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) {
    private val mutex = Mutex()
    
    val pageCount: Int
        get() = renderer.pageCount
    
    /**
     * Renders a specific page to a bitmap with optional zoom.
     * @param pageIndex Zero-based page index
     * @param scaleFactor Scale factor for rendering (1.0 = 100%)
     * @param maxWidth Maximum width for the rendered bitmap (0 = no limit)
     * @param maxHeight Maximum height for the rendered bitmap (0 = no limit)
     * @return Rendered bitmap or null if error
     */
    suspend fun renderPage(
        pageIndex: Int,
        scaleFactor: Float = 1.0f,
        maxWidth: Int = 0,
        maxHeight: Int = 0
    ): Bitmap? = mutex.withLock {
        coroutineContext.ensureActive()

        if (pageIndex !in 0..<pageCount) {
            return@withLock null
        }
        
        return@withLock try {
            val page = renderer.openPage(pageIndex)
            try {
                // Ensure we haven't been cancelled before rendering
                coroutineContext.ensureActive()
                
                val pageWidth = (page.width * scaleFactor).toInt()
                val pageHeight = (page.height * scaleFactor).toInt()
                
                // Calculate final dimensions respecting max constraints
                val finalWidth = if (maxWidth in 1..<pageWidth) {
                    maxWidth
                } else {
                    pageWidth
                }
                
                val finalHeight = if (maxHeight in 1..<pageHeight) {
                    maxHeight
                } else {
                    pageHeight
                }
                
                val bitmap = createBitmap(finalWidth, finalHeight)
                
                // Apply scale if needed
                if (scaleFactor != 1.0f || finalWidth != pageWidth || finalHeight != pageHeight) {
                    val matrix = Matrix().apply {
                        val scaleX = finalWidth.toFloat() / page.width
                        val scaleY = finalHeight.toFloat() / page.height
                        postScale(scaleX, scaleY)
                    }
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                } else {
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
                
                bitmap
            } finally {
                page.close()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Gets the dimensions of a page without rendering it.
     */
    suspend fun getPageSize(pageIndex: Int): Pair<Int, Int>? = mutex.withLock {
        coroutineContext.ensureActive()

        if (pageIndex !in 0..<pageCount) {
            return@withLock null
        }
        
        return@withLock try {
            val page = renderer.openPage(pageIndex)
            try {
                Pair(page.width, page.height)
            } finally {
                page.close()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Closes the renderer and releases resources.
     * This method ensures that the renderer is properly closed even if there are
     * warnings from the system about pages not being fully destroyed.
     */
    fun close() {
        try {
            // Close the renderer first
            // Note: Android may log a warning if pages are still open, but this is
            // safe to ignore as long as we ensure all operations are cancelled before
            // calling close() (which is handled by PdfPageRenderer.dispose())
            renderer.close()
        } catch (e: Exception) {
            // Log but don't throw - we still want to close the file descriptor
            e.printStackTrace()
        } finally {
            // Always close the file descriptor
            try {
                fileDescriptor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    companion object {
        /**
         * Creates a PdfRendererManager from a File.
         * @throws IOException if the file cannot be opened
         */
        @Throws(IOException::class)
        fun create(file: File): PdfRendererManager {
            val fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(fileDescriptor)
            return PdfRendererManager(fileDescriptor, renderer)
        }
        
        /**
         * Creates a PdfRendererManager from a ParcelFileDescriptor.
         * @throws IOException if the descriptor cannot be used
         */
        @Throws(IOException::class)
        fun create(fileDescriptor: ParcelFileDescriptor): PdfRendererManager {
            val renderer = PdfRenderer(fileDescriptor)
            return PdfRendererManager(fileDescriptor, renderer)
        }
    }
}

