package com.fmaestre98.pdfviewer.pdfViewer.text


import android.content.Context
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import java.io.File
import com.fmaestre98.pdfviewer.pdfViewer.text.PdfTextStripper as CustomTextStripper

/**
 * Extractor de texto y coordenadas usando PDFBox con tu CustomTextStripper.
 * Maneja la conversión de coordenadas del PDF a pantalla.
 */
class PdfTextExtractor private constructor(
    private val document: PDDocument,
    private val originalPageSize: Pair<Int, Int> // Tamaño original de la página del PDF
) : AutoCloseable {

    companion object {
        /**
         * Crea un extractor para un archivo PDF.
         * @param context Contexto de Android
         * @param pdfFile Archivo PDF
         * @param pageSize Tamaño original de la página (obtenido de PdfRendererManager.getPageSize)
         */
        suspend fun create(
            context: Context,
            pdfFile: File,
            pageSize: Pair<Int, Int>
        ): PdfTextExtractor = withContext(Dispatchers.IO) {
            try {
                // Cargar documento con PDFBox
                val document = PDDocument.load(pdfFile)
                PdfTextExtractor(document, pageSize)
            } catch (e: Exception) {
                throw RuntimeException("Error al cargar PDF para extracción de texto", e)
            }
        }
    }

    /**
     * Extrae texto y coordenadas de una página específica.
     * @param pageIndex Índice de la página (0-based)
     * @return PageModel con las coordenadas del texto
     */
    suspend fun extractPageModel(pageIndex: Int): PageModel? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= document.numberOfPages) {
            return@withContext null
        }

        try {
            // Usar tu PdfTextStripper personalizado
            val stripper = CustomTextStripper(
                page = pageIndex,
                lineIdStart = 0,
                wordIdStart = 0,
                charIdStart = 0
            )

            stripper.startPage = pageIndex + 1  // PDFBox usa 1-based
            stripper.endPage = pageIndex + 1

            // Extraer texto con coordenadas
            val text = stripper.getText(document)
            println("my-logs extractPageModel $pageIndex $text")
            // Obtener las coordenadas que tu PdfTextStripper calculó
            val lines = stripper.getTextCoordinates()

            // Convertir las líneas a nuestro modelo si es necesario
            // (Asumiendo que tu PdfTextStripper ya devuelve ArrayList<PdfLine>)
            return@withContext PageModel(
                coordinates = lines,
                relativeSizeCalculated = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Cierra el documento PDF.
     */
    override fun close() {
        try {
            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Ajusta las coordenadas del texto al tamaño de pantalla actual.
     * @param pageModel Modelo con coordenadas en espacio PDF
     * @param targetWidth Ancho objetivo de renderizado
     * @param targetHeight Alto objetivo de renderizado
     */
    fun adjustCoordinatesToScreenSize(
        pageModel: PageModel,
        targetWidth: Float,
        targetHeight: Float,
        zoomLevel: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): PageModel {

        val (pdfWidth, pdfHeight) = originalPageSize
        val scaleX = targetWidth / pdfWidth * zoomLevel
        val scaleY = targetHeight / pdfHeight * zoomLevel
        println("my-logs called adjustCoordinatesToScreenSize pdfWidth=$pdfWidth pdfHeight=$pdfHeight");
        pageModel.coordinates.forEach { line ->
            line.relatedPosition.set(
                line.position.x * scaleX + offsetX,
                line.position.y * scaleY + offsetY
            )
            line.relatedSize.set(
                line.size.width * scaleX,
                line.size.height * scaleY
            )

            // Actualizar rectángulo
            line.rect.set(
                line.relatedPosition.x,
                line.relatedPosition.y,
                line.relatedPosition.x + line.relatedSize.width,
                line.relatedPosition.y + line.relatedSize.height
            )

            // Ajustar palabras
            line.words.forEach { word ->
                val originalY = word.position.y
                word.relatedPosition.set(
                    word.position.x * scaleX,
                    word.position.y * scaleY
                )
                word.relatedSize.set(
                    word.size.width * scaleX,
                    word.size.height * scaleY
                )
                word.rect.set(
                    word.relatedPosition.x,
                    word.relatedPosition.y,
                    word.relatedPosition.x + word.relatedSize.width,
                    word.relatedPosition.y + word.relatedSize.height
                )

                // Ajustar caracteres
                word.characters.forEach { char ->
                    char.relatedPosition.set(
                        char.topPosition.x * scaleX,
                        char.topPosition.y * scaleY
                    )
                    char.relatedSize.set(
                        char.size.width * scaleX,
                        char.size.height * scaleY
                    )
                    char.rect.set(
                        char.relatedPosition.x,
                        char.relatedPosition.y,
                        char.relatedPosition.x + char.relatedSize.width,
                        char.relatedPosition.y + char.relatedSize.height
                    )
                }
            }
        }

        pageModel.relativeSizeCalculated = true
        return pageModel
    }
}