package android.assignment.infinityquiz.viewmodel

import android.assignment.infinityquiz.network.Result
import android.assignment.infinityquiz.db.QuestionEntity
import android.assignment.infinityquiz.repository.QuestionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val repository: QuestionRepository
) : ViewModel() {

    // StateFlow for questions
    private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val questions: StateFlow<List<QuestionEntity>> get() = _questions

    // StateFlow for bookmarked questions
    private val _bookmarkedQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val bookmarkedQuestions: StateFlow<List<QuestionEntity>> get() = _bookmarkedQuestions

    init {
        viewModelScope.launch {
            repository.getQuestions()
                .collect { questionList ->
                    _questions.value = questionList
                }
        }

        viewModelScope.launch {
            repository.getBookmarkedQuestions()
                .collect { bookmarkedList ->
                    _bookmarkedQuestions.value = bookmarkedList
                }
        }

        refreshQuestions()
    }

    fun refreshQuestions(onSuccess: (() -> Unit)? = null, onError: (() -> Unit)? = null) {
        // Refresh questions on initialization
        viewModelScope.launch {
            repository.refreshQuestions().run {
                when (this) {
                    is Result.Success -> {
                        onSuccess?.invoke()
                    }

                    is Result.Error -> {
                        onError?.invoke()
                    }

                    else -> {}
                }
            }
        }
    }

    fun toggleBookmark(question: QuestionEntity) {
        viewModelScope.launch { repository.toggleBookmark(question) }
    }

    // List to store answers (correct or incorrect) for each question
    private val _recentAnswers = mutableListOf<Boolean>()
    val recentAnswers: List<Boolean> get() = _recentAnswers

    private val _showDelight = MutableStateFlow(false)
    val showDelight: StateFlow<Boolean> get() = _showDelight

    fun onQuestionAnswered(isCorrect: Boolean) {
        _recentAnswers.add(isCorrect)
    }

    fun checkAndShowDelight() {
        // Check if we've completed 5 questions
        if (_recentAnswers.size > 0 && _recentAnswers.size % 5 == 0) {
            _showDelight.value = true  // Trigger the delight
        } else {
            _showDelight.value = false
        }
    }

    fun resetDelight() {
        _showDelight.value = false
    }

    fun getLastFiveStats(): String {
        val lastFive = recentAnswers.takeLast(5)
        val correctCount = lastFive.count { it }
        return "Last 5 Questions: $correctCount correct out of 5"
    }
}
