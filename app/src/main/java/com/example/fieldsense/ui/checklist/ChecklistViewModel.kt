package com.example.fieldsense.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fieldsense.data.model.Answer
import com.example.fieldsense.data.model.Template
import com.example.fieldsense.data.model.VisitChecklist
import com.example.fieldsense.data.repository.ChecklistRepository
import com.example.fieldsense.data.repository.QuestionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChecklistViewModel(private val repository: ChecklistRepository, private val questionRepository: QuestionRepository) : ViewModel() {

    private val checklistsCache = mutableMapOf<Int, StateFlow<List<VisitChecklist>>>()
    private val answersCache = mutableMapOf<Int, StateFlow<List<Answer>>>()

    fun getChecklistsForVisit(visitId: Int): StateFlow<List<VisitChecklist>> {
        return checklistsCache.getOrPut(visitId) {
            repository.getChecklistsForVisit(visitId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun getAnswersForChecklist(checklistId: Int): StateFlow<List<Answer>> {
        return answersCache.getOrPut(checklistId) {
            repository.getAnswersForChecklist(checklistId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun insertChecklistWithAnswers(checklist: VisitChecklist, answers: List<Answer>) {
        viewModelScope.launch {
            insertChecklistWithAnswersSuspend(checklist, answers)
        }
    }

    suspend fun insertChecklistWithAnswersSuspend(checklist: VisitChecklist, answers: List<Answer>) {
        repository.insertChecklistWithAnswers(checklist, answers)
    }

    fun deleteChecklist(checklist: VisitChecklist) {
        viewModelScope.launch {
            repository.deleteChecklist(checklist)
        }
    }

    fun onNetworkRestored() {
        viewModelScope.launch {
            repository.syncPendingChecklists()
        }
    }

    fun cleanupDuplicateAnswers(checklistId: Int) {
        viewModelScope.launch {
            repository.cleanupDuplicateAnswers(checklistId)
        }
    }

    fun createChecklistFromTemplate(visitId: Int, template: Template) {
        viewModelScope.launch {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val checklist = VisitChecklist(
                visitId = visitId,
                templateId = template.id,
                templateName = template.name,
                date = date
            )
            // Buscar as perguntas do template para criar as answers vazias
            val questions = questionRepository.getQuestionsForTemplateOnce(template.id)
            val emptyAnswers = questions.map { question ->
                Answer(
                    checklistId = 0, // será substituído pelo id gerado na inserção
                    questionId = question.id,
                    questionText = question.text,
                    questionType = question.type,
                    value = ""
                )
            }
            repository.insertChecklistWithAnswers(checklist, emptyAnswers)
        }
    }
}

class ChecklistViewModelFactory(
    private val repository: ChecklistRepository,
    private val questionRepository: QuestionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChecklistViewModel(repository, questionRepository) as T
    }
}