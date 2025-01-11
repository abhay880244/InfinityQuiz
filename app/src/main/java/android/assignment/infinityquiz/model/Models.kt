package android.assignment.infinityquiz.model

data class QuestionResponse(
    val uuidIdentifier: String,
    val questionType: String,
    val question: String,
    val option1: String,
    val option2: String,
    val option3: String,
    val option4: String,
    val correctOption: Int,
    val sort: Int,
    val solution: List<Solution>
)

data class Solution(
    val contentType: String,
    val contentData: String
)
