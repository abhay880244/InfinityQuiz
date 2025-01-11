package android.assignment.infinityquiz.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val uuidIdentifier: String,
    val questionType: String,
    val question: String,
    val options: String, // JSON string for the list of options
    val correctOption: Int,
    val sort: Int,
    val solution: String, // JSON string for the list of solutions
    val isBookmarked: Boolean = false
)
