package com.example.fieldsense.ui.templates

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fieldsense.data.model.Question
import com.example.fieldsense.data.model.Template
import com.example.fieldsense.data.repository.TemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.onEach
class TemplateViewModel(private val repository: TemplateRepository) : ViewModel() {

    val templates: StateFlow<List<Template>> = repository.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertTemplateWithQuestions(name: String, description: String, questions: List<Question>) {
        viewModelScope.launch {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val template = Template(name = name, description = description, date = date)
            repository.insertTemplateWithQuestions(template, questions)
        }
    }

    fun updateTemplateWithQuestions(template: Template, questions: List<Question>) {
        viewModelScope.launch {
            repository.updateTemplateWithQuestions(template, questions)
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun syncPendingTemplates() {
        viewModelScope.launch {
            repository.syncPendingTemplates()
        }
    }

    fun onNetworkRestored() {
        viewModelScope.launch {
            repository.syncPendingTemplates()
        }
    }
    fun getQuestionsForTemplate(templateId: Int): StateFlow<List<Question>> {
        Log.d("TemplateVM", "A buscar perguntas para templateId: $templateId")
        return repository.getQuestionsForTemplate(templateId)
            .onEach { Log.d("TemplateVM", "Perguntas recebidas: ${it.size}") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }}

class TemplateViewModelFactory(
    private val repository: TemplateRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TemplateViewModel(repository) as T
    }
}