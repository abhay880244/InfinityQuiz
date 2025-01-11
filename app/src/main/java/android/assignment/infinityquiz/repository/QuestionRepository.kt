package android.assignment.infinityquiz.repository

import android.assignment.infinityquiz.model.QuestionResponse
import android.assignment.infinityquiz.network.Result
import android.assignment.infinityquiz.db.QuestionDao
import android.assignment.infinityquiz.db.QuestionEntity
import android.assignment.infinityquiz.network.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val apiService: ApiService,
    private val questionDao: QuestionDao
) {

    // Flow to get all questions
    fun getQuestions(): Flow<List<QuestionEntity>> = questionDao.getQuestions()

    // Flow to fetch only bookmarked questions
    fun getBookmarkedQuestions(): Flow<List<QuestionEntity>> = questionDao.getBookmarkedQuestions()

    // Refresh questions from the API but retain bookmark status
    suspend fun refreshQuestions(): Result<String> {
        return try {
            Result.Loading
            var apiQuestions: List<QuestionResponse>? = null

            apiQuestions = apiService.getQuestions()

            // Fetch all current questions from the database
            val existingQuestions = questionDao.getQuestions().first()

            // Map API questions to entities
            val questionsToInsert = apiQuestions?.map { apiQuestion ->
                // Find the existing question by UUID and retain bookmark status
                val existingQuestion =
                    existingQuestions.find { it.uuidIdentifier == apiQuestion.uuidIdentifier }

                // If the question exists, we retain the bookmark status, otherwise set it as false
                QuestionEntity(
                    uuidIdentifier = apiQuestion.uuidIdentifier,
                    questionType = apiQuestion.questionType,
                    question = apiQuestion.question,
                    options = Gson().toJson(
                        listOf(
                            apiQuestion.option1,
                            apiQuestion.option2,
                            apiQuestion.option3,
                            apiQuestion.option4
                        )
                    ),
                    correctOption = apiQuestion.correctOption,
                    sort = apiQuestion.sort,
                    solution = Gson().toJson(apiQuestion.solution),
                    isBookmarked = existingQuestion?.isBookmarked
                        ?: false  // Retain bookmark status
                )
            }

            // Insert new questions or update existing ones
            questionsToInsert?.let { questionDao.insertQuestions(it) }
            Result.Success("Questions refreshed successfully")
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Toggle bookmark status for a given question
    suspend fun toggleBookmark(question: QuestionEntity) {
        questionDao.updateQuestion(question.copy(isBookmarked = !question.isBookmarked))
    }

}
