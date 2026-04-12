package com.fmaestre98.pdfviewer.pdfViewer.text

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.fmaestre98.pdfviewer.pdfViewer.model.PageModel
import com.fmaestre98.pdfviewer.pdfViewer.model.PdfWord
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initializationError: String? = null

    // Callback para actualizar la UI (resaltar palabra)
    var onWordHighlight: ((PdfWord?) -> Unit)? = null

    var onInitializationSuccess: (() -> Unit)? = null

    // Callback para errores de inicialización
    var onInitializationError: ((String) -> Unit)? = null

    // Mapeo de índices de caracteres a objetos PdfWord
    private var charIndexToWordMap:  Map<Int, PdfWord> = emptyMap()

    init {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Intentar configurar español como idioma predeterminado
                val result = tts?.setLanguage(Locale("es", "ES"))

                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    initializationError = "Datos de idioma español no disponibles"
                    onInitializationError?.invoke(initializationError!!)
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    initializationError = "Idioma español no soportado"
                    onInitializationError?.invoke(initializationError!!)
                } else {
                    isInitialized = true
                    setupUtteranceListener()
                    onInitializationSuccess?.invoke()
                }
            } else {
                initializationError = "Error al inicializar Text-to-Speech"
                onInitializationError?.invoke(initializationError!!)
            }
        })
    }

    // Método para verificar si TTS está disponible
    fun isAvailable(): Boolean {
        return isInitialized && initializationError == null
    }

    // Obtener el error de inicialización
    fun getInitializationError(): String? {
        return initializationError
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS", "Comenzó a hablar: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TTS", "Terminó de hablar: $utteranceId")
                onWordHighlight?.invoke(null) // Limpiar resaltado al terminar
            }

            override fun onError(utteranceId: String?) {
                Log.e("TTS", "Error al hablar: $utteranceId")
                onWordHighlight?.invoke(null)
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
                val word = findWordForIndex(start)
                onWordHighlight?.invoke(word)
            }
        })
    }

    fun speakPage(pageModel: PageModel) {
        if (!isInitialized) {
            Log.e("TTS", "TTS no inicializado")
            return
        }

        // Detener cualquier lectura previa
        stop()

        // 1. Aplanar todas las palabras en una lista lineal
        val allWords = pageModel.coordinates.flatMap { it.words }
        println(allWords)
        // 2. Construir el texto completo y crear el mapa de índices
        val stringBuilder = StringBuilder()
        val map = mutableMapOf<Int, PdfWord>()

        var currentIndex = 0

        allWords.forEach { word ->
            val wordText = word.text
            // Guardamos que desde currentIndex hasta currentIndex + length es esta palabra
            for (i in currentIndex until (currentIndex + wordText.length)) {
                map[i] = word
            }

            stringBuilder.append(wordText).append(" ")
            currentIndex += wordText.length + 1 // +1 por el espacio
        }

        charIndexToWordMap = map
        val fullText = stringBuilder.toString()

        // 3. Hablar
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "page_${System.currentTimeMillis()}")
        tts?.speak(fullText, TextToSpeech.QUEUE_FLUSH, params, "page_${System.currentTimeMillis()}")
    }

    private fun findWordForIndex(index: Int): PdfWord? {
        return charIndexToWordMap[index] ?: charIndexToWordMap[index - 1]
    }

    fun stop() {
        tts?.stop()
        onWordHighlight?.invoke(null)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}