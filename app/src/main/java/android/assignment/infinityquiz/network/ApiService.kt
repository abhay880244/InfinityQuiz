package android.assignment.infinityquiz.network

import android.assignment.infinityquiz.model.QuestionResponse
import retrofit2.http.GET

interface ApiService {
    @GET("v3/d2d115f7-6f3e-4f67-aeb7-657ba801d57a")
    suspend fun getQuestions(): List<QuestionResponse>
}
