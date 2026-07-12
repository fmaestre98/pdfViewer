package com.fmaestre98.pdfviewer.pdfViewer.text

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class SearchResult(
    val pageIndex: Int,
    val snippet: String,
    val matchStartIndex: Int
)

class PdfSearchManager {

    /**
     * Searches for text within a PDF file and emits results progressively.
     * @param pdfFile The PDF file to search in
     * @param query The text to search for
     * @param snippetLength Maximum length of the text snippet around the match
     * @return A Flow that emits lists of results (grouped by page)
     */
    fun search(pdfFile: File, query: String, snippetLength: Int = 100): Flow<List<SearchResult>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val document = try {
            PDDocument.load(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return@flow
        }

        try {
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages
            val lowerQuery = query.lowercase()

            for (page in 1..totalPages) {
                currentCoroutineContext().ensureActive()
                
                stripper.startPage = page
                stripper.endPage = page
                
                val pageText = stripper.getText(document)
                if (pageText.isNotEmpty()) {
                    val lowerPageText = pageText.lowercase()
                    var startIndex = 0
                    val results = mutableListOf<SearchResult>()
                    
                    while (startIndex < lowerPageText.length) {
                        val matchIndex = lowerPageText.indexOf(lowerQuery, startIndex)
                        if (matchIndex != -1) {
                            val startSnippet = maxOf(0, matchIndex - snippetLength / 2)
                            val endSnippet = minOf(pageText.length, matchIndex + query.length + snippetLength / 2)
                            var snippet = pageText.substring(startSnippet, endSnippet)
                            
                            if (startSnippet > 0) snippet = "...$snippet"
                            if (endSnippet < pageText.length) snippet = "$snippet..."
                            
                            snippet = snippet.replace(Regex("\\s+"), " ").trim()
                            
                            results.add(SearchResult(
                                pageIndex = page - 1,
                                snippet = snippet,
                                matchStartIndex = matchIndex
                            ))
                            startIndex = matchIndex + query.length
                        } else {
                            break
                        }
                    }
                    
                    if (results.isNotEmpty()) {
                        emit(results)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }.flowOn(Dispatchers.IO)
}
