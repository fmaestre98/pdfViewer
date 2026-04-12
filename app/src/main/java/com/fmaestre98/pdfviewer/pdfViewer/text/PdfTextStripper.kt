package com.fmaestre98.pdfviewer.pdfViewer.text

import android.graphics.PointF
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfChar
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfLine
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord
import com.fmaestre98.pdfviewer.pdfViewer.model.Size

@Suppress("UNCHECKED_CAST")
class PdfTextStripper(
    private var page : Int,
    private var lineIdStart : Int =0,
    private var wordIdStart : Int =0,
    private var charIdStart : Int =0,
) : PDFTextStripper() {

    private val textCoordinators = arrayListOf<PdfLine>()

    fun reset(){
        textCoordinators.clear()

    }


    fun updateInfo(
        page: Int = 0,
        lineIdStart: Int = 0,
        wordIdStart: Int = 0,
        charIdStart: Int = 0,
    ) {
        this.page = page
        this.lineIdStart = lineIdStart
        this.wordIdStart = wordIdStart
        this.charIdStart = charIdStart
    }


    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
//        "text $text => $textPositions".log("textPositions")
        super.writeString(text, textPositions)
        if (text?.trim().isNullOrEmpty()|| textPositions == null) return
        lineIdStart++
        try {
            val result = extractWords(text!!, textPositions)
            if (result != null){
                val words = result[WORDS] as ArrayList<PdfWord>
                val size = result[SIZE] as Size
                val position = result[POSITION] as PointF
                val pdfLine = PdfLine(lineIdStart,text, position, size)
                pdfLine.words.addAll(words)
                textCoordinators.add(pdfLine)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//
//    private fun extractWords(text: String, textPosition: List<TextPosition>) : HashMap<String,Any>?{
//        val result : HashMap<String, Any> = hashMapOf()
//        val pdfWords = ArrayList<PdfWord>()
//        var wordStrings = text.split(" ")
//
//        if (wordStrings.isEmpty() && text.isNotEmpty()){
//            wordStrings = listOf(text)
//        }
//
//        Log.e(TAG, "extractWords() called with: text = ${text.getOrNull(0)}, words : $wordStrings ,  textPosition = ${textPosition.getOrNull(0)}")
//        var lastIndex = 0
//        wordStrings.forEach {
//            val startIndex = lastIndex
//            val endIndex = startIndex + it.length
//            lastIndex = endIndex+1
//            val textPosList = if (endIndex + 1 > textPosition.size) textPosition.subList(
//                startIndex,
//                textPosition.size
//            ) else textPosition.subList(startIndex, endIndex + 1)
//            val charResult =extractChars(textPosList)
//            if (charResult != null){
//                val chars = charResult[CHARS] as List<PdfChar>
//                val position = charResult[POSITION] as PointF
//                val size = charResult[SIZE] as Size
//                val word = PdfWord(
//                    wordIdStart,
//                    lineIdStart,
//                    it,
//                    position, size
//                )
//                word.characters.addAll(chars)
//                pdfWords.add(word)
//                wordIdStart++
//            }
//
//        }
//        if (pdfWords.isNotEmpty()){
//            val startWord = pdfWords[0]
//            val endWord = pdfWords[pdfWords.lastIndex]
//            val y = minOf(startWord.position.y, endWord.position.y)
//            val heightStartY = (startWord.position.y + startWord.size.height)
//            val heightEndY = (endWord.position.y + endWord.size.height)
//            val maxHeightY = maxOf(heightStartY,heightEndY)
//            val height = maxHeightY - y
//            val width = (endWord.position.x+endWord.size.width) - startWord.position.x
//
//            result[WORDS] = pdfWords
//            result[POSITION] = PointF(startWord.position.x,y)
//            result[SIZE] = Size(width, height)
//            return result
//        }
//        return  null
//    }

    private fun extractWords(text: String, textPosition: List<TextPosition>) : HashMap<String,Any>?{
        val result : HashMap<String, Any> = hashMapOf()
        val pdfWords = ArrayList<PdfWord>()
        val wordStrings = splitByWords(textPosition)
        wordStrings.forEach {
            val charResult = extractChars(it.textPositions)
            if (charResult != null){
                val chars = charResult[CHARS] as List<PdfChar>
                val position = charResult[POSITION] as PointF
                val size = charResult[SIZE] as Size
                val word = PdfWord(
                    wordIdStart,
                    lineIdStart,
                    it.word,
                    position, size
                )
                word.characters.addAll(chars)
                pdfWords.add(word)
                wordIdStart++
            }
        }
        if (pdfWords.isNotEmpty()){
            val startWord = pdfWords[0]
            val endWord = pdfWords[pdfWords.lastIndex]
            val y = minOf(startWord.position.y, endWord.position.y)
            val heightStartY = (startWord.position.y + startWord.size.height)
            val heightEndY = (endWord.position.y + endWord.size.height)
            val maxHeightY = maxOf(heightStartY,heightEndY)
            val height = maxHeightY - y
            val width = (endWord.position.x+endWord.size.width) - startWord.position.x

            result[WORDS] = pdfWords
            result[POSITION] = PointF(startWord.position.x,y)
            result[SIZE] = Size(width, height)
            return result
        }
        return  null
    }

    private fun splitByWords(textPosition: List<TextPosition>): List<WordExtractionResult> {
        val tempList = arrayListOf<TextPosition>()
        val wordExtractionResult = arrayListOf<WordExtractionResult>()
        var word = ""
        for (pos in textPosition){
            tempList.add(pos)
            if (pos.unicode[0].isRightToLeft()){
                word = pos.unicode + word
            }else {
                word += pos.unicode
            }
            if (pos.unicode.isBlank()){
                wordExtractionResult.add(WordExtractionResult(word,tempList.toList()))
                tempList.clear()
                word = ""
            }
        }
        if (tempList.isNotEmpty()){
            wordExtractionResult.add(WordExtractionResult(word,tempList.toList()))
        }
        return wordExtractionResult
    }

    data class WordExtractionResult(
        val word: String,
        val textPositions: List<TextPosition>
    ){
        override fun toString(): String {
            return word
        }
    }

    private fun extractChars(textPosition: List<TextPosition>): HashMap<String, Any>? {
        val result: HashMap<String, Any> = hashMapOf()
        val pdfChars = ArrayList<PdfChar>()

        // Variables para calcular el BoundingBox total de la palabra
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        textPosition.forEach { pos ->
            // 1. Obtener métricas reales de la fuente para evitar el padding mágico
            // Si no podemos obtener el font descriptor, usamos una aproximación estándar
            // CapHeight suele ser ~0.7 del tamaño, Descent ~0.2
            val font = pos.font
            val fontSize = pos.fontSizeInPt

            // El 'yDirAdj' en PDFBox es usualmente la BASELINE.
            // Para obtener el TOP, restamos el Ascenso (capHeight o ascent).
            // Para obtener el BOTTOM, sumamos el Descenso.

            // Intento de obtener ascenso/descenso real
            val ascent = if (font?.fontDescriptor != null) {
                (font.fontDescriptor.ascent / 1000f) * fontSize
            } else {
                pos.height // Fallback
            }

            // El descenso suele ser negativo en PDFBox, por eso lo restamos o usamos abs
            val descent = if (font?.fontDescriptor != null) {
                kotlin.math.abs((font.fontDescriptor.descent / 1000f) * fontSize)
            } else {
                0f // Fallback asumiendo que height cubre todo
            }

            // Coordenada Y en PDF (0 abajo) vs Pantalla (0 arriba) es confuso,
            // pero TextPosition ya suele dar coordenadas ajustadas a "User Space".
            // Asumiremos que yDirAdj es la Baseline.

            // Ajuste Fino:
            // Top = Baseline - Ascent
            // Bottom = Baseline + Descent
            val topY = pos.yDirAdj - ascent
            val bottomY = pos.yDirAdj + descent
            val leftX = pos.xDirAdj
            val rightX = pos.xDirAdj + pos.width

            // Actualizar límites de la palabra completa
            minX = minOf(minX, leftX)
            minY = minOf(minY, topY)
            maxX = maxOf(maxX, rightX)
            maxY = maxOf(maxY, bottomY)

            val size = Size(pos.width, (bottomY - topY))
            val topPosition = PointF(leftX, topY)
            val bottomPosition = PointF(leftX, bottomY) // Solo referencial

            val char = PdfChar(
                charIdStart,
                lineIdStart,
                wordIdStart,
                pos.unicode,
                topPosition,
                bottomPosition,
                size,
                page
            )
            pdfChars.add(char)
            charIdStart++
        }

        if (pdfChars.isNotEmpty()) {
            val width = maxX - minX
            val height = maxY - minY

            // Guardamos la posición TOP-LEFT de la palabra
            val wordPosition = PointF(minX, minY)

            result[CHARS] = pdfChars
            result[POSITION] = wordPosition
            result[SIZE] = Size(width, height)
            return result
        }
        return null
    }


    fun getLastLineId(): Int {
        return lineIdStart
    }
    fun getLastWordId(): Int {
        return wordIdStart
    }
    fun getLastCharId(): Int {
        return charIdStart
    }

    fun getTextCoordinates(): ArrayList<PdfLine> {
//        textCoordinators.sortBy { it.position.y }
        return textCoordinators
    }

    private fun Char.isRightToLeft(): Boolean {
        val charDir = Character.getDirectionality(this)
        return charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                || charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                || charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
    }

    companion object {
        private const val WORDS = "WORDS"
        private const val CHARS = "CHARS"
        private const val POSITION = "POSITION"
        private const val SIZE = "SIZE"
        private const val TAG = "PdfTextStripper"
    }

}


