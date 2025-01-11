package android.assignment.infinityquiz.composable

import android.assignment.infinityquiz.viewmodel.QuizViewModel
import android.assignment.infinityquiz.model.Solution
import android.content.Context
import android.content.Intent
import android.text.Html
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun AnswerExplanationScreen(
    questionIndex: Int,
    selectedAnswer: Int,
    navController: NavController,
    filter: String,
    viewModel: QuizViewModel = hiltViewModel()
) {

    val questions by viewModel.questions.collectAsState(emptyList())
    val bookmarkedQuestions by viewModel.bookmarkedQuestions.collectAsState(emptyList())

    val displayedQuestions = if (filter == "bookmarked") bookmarkedQuestions else questions
    val question = displayedQuestions.getOrNull(questionIndex)
    val context = LocalContext.current

    val lazyColumnState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = lazyColumnState
    ) {
        item {
            QuestionAndAnswerLayout(
                question,
                context,
                selectedAnswer = selectedAnswer,
                isInteractive = false,
                selectedAnswerCallback = null,
                navigateCallback = null
            )
        }

        item {
            question?.let {
                Text("Correct Answer: ${question.correctOption}")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Answer Explanation", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Display solution content based on its type
        question?.let {
            val solutions = Gson().fromJson(
                question.solution,
                object : TypeToken<List<Solution>>() {}.type
            ) as List<Solution>
            solutions.forEach { solution ->
                item {
                    RenderContentBasedOnType(solution.contentType, solution.contentData)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        item {
            question?.let {
                Button(onClick = {
                    if (questionIndex + 1 > displayedQuestions.size - 1) {
                        Toast.makeText(context, "No more questions", Toast.LENGTH_SHORT).show()
                    } else {
                        navigateToNextQuestion(navController, questionIndex, filter)
                    }
                }) {
                    Text("Next Question")
                }
                Spacer(modifier = Modifier.height(8.dp))
                BookMarkButton(viewModel, question)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { shareQuestionText(context, question.question) }) {
                    Text(text = "Share")
                }
            }
        }
    }
    LaunchedEffect(question) {
        question?.let {
            lazyColumnState.animateScrollToItem(1, scrollOffset = -600)
        }
    }
}

@Composable
fun RenderContentBasedOnType(type: String, content: String) {
    when (type) {
        "text" -> Text(text = content)
        "htmlText" -> AndroidView(factory = { context ->
            TextView(context).apply {
                text = Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)
            }
        })

        "image" -> Image(
            painter = rememberAsyncImagePainter(content),
            contentDescription = "Solution Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        )
    }

}


fun shareQuestionText(context: Context, questionText: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, questionText)
        type = "text/plain"
    }
    val chooser = Intent.createChooser(shareIntent, "Share Question")
    context.startActivity(chooser)
}
