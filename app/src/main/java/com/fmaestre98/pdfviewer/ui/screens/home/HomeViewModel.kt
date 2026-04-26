package com.fmaestre98.pdfviewer.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmaestre98.pdfviewer.R
import com.fmaestre98.pdfviewer.models.Book
import com.fmaestre98.pdfviewer.repository.PDFRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pdfRepository: PDFRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeBooks()
    }

    private fun observeBooks() {
        viewModelScope.launch {
            pdfRepository.observeBooks()
                .onStart { _state.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                    _events.send(HomeEvent.ShowSnackbar(R.string.home_error_loading))
                }
                .collect { books ->
                    _state.update { it.copy(books = books, isLoading = false, error = null) }
                }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnBookClick -> {
                viewModelScope.launch {
                    _events.send(HomeEvent.NavigateToReader(action.uri))
                }
            }
            is HomeAction.OnDeleteBook -> {
                viewModelScope.launch {
                    try {
                        pdfRepository.deleteBook(action.uri)
                    } catch (e: Exception) {
                        _events.send(HomeEvent.ShowSnackbar(R.string.home_error_loading)) // Or a specific delete error
                    }
                }
            }
            is HomeAction.OnPdfSelected -> {
                addBook(action.uri, action.displayName, action.sizeBytes)
            }
        }
    }

    private fun addBook(uri: Uri, displayName: String, sizeBytes: Long) {
        viewModelScope.launch {
            try {
                pdfRepository.addBook(uri, displayName, sizeBytes)
                _events.send(HomeEvent.ShowSnackbar(R.string.home_pdf_added))
            } catch (e: Exception) {
                _events.send(HomeEvent.ShowSnackbar(R.string.home_error_adding))
            }
        }
    }
}
