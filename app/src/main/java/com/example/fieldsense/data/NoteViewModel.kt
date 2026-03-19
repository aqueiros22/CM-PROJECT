package com.example.fieldsense.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    private val notesCache = mutableMapOf<Int, StateFlow<List<Note>>>()
    /*fun getNotesForVisit(visitId: Int): StateFlow<List<Note>> =
        repository.getNotesForVisit(visitId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
*/
    fun getNotesForVisit(visitId: Int): StateFlow<List<Note>> {
        return notesCache.getOrPut(visitId) {
            repository.getNotesForVisit(visitId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }
    fun insertNote(visitId: Int, content: String) {
        viewModelScope.launch {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            repository.insertNote(Note(visitId = visitId, content = content, date = date))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun syncPendingNotes() {
        viewModelScope.launch {
            repository.syncPendingNotes()
        }
    }
    fun onNetworkRestored() {
        viewModelScope.launch {
            repository.syncPendingNotes()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NoteViewModel(repository) as T
    }
}