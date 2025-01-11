package android.assignment.infinityquiz.composable

import android.assignment.infinityquiz.viewmodel.QuizViewModel
import android.assignment.infinityquiz.R
import android.assignment.infinityquiz.db.QuestionEntity
import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

@Composable
fun QuestionScreen(
    navController: NavController,
    viewModel: QuizViewModel,
    questionIndex: Int = 0,
    filter: String,
    timeLimit: Int = 30
) {
    val context = LocalContext.current

    LaunchedEffect(filter) {
        if (filter == "bookmarked") {
            if (viewModel.bookmarkedQuestions.value.isEmpty()) {
                Toast.makeText(context, "No Questions Bookmarked", Toast.LENGTH_SHORT).show()
                navController.navigate("home")
            }
        } else {
            if (viewModel.questions.value.isEmpty()) {
                viewModel.refreshQuestions(onError = {
                    Toast.makeText(context, "Sorry no questions available", Toast.LENGTH_SHORT).show()
                    navController.navigate("home")
                })
            }
        }
    }
    val questions by viewModel.questions.collectAsState(emptyList())
    val bookmarkedQuestions by viewModel.bookmarkedQuestions.collectAsState(emptyList())

    val displayedQuestions = if (filter == "bookmarked") bookmarkedQuestions else questions
    val question = displayedQuestions.getOrNull(questionIndex)

    val selectedAnswer = remember { mutableStateOf(-1) }
    val timerProgress = remember { mutableStateOf(1f) }
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.background_music) }
    var isMuted by remember { mutableStateOf(false) }
    var timerRemaining by remember { mutableStateOf(timeLimit) }

    LaunchedEffect(timerRemaining) {
        if (timerRemaining > 0) {
            delay(1000L)
            timerRemaining -= 1
            timerProgress.value = timerRemaining.toFloat() / timeLimit
        } else {
            if (questionIndex + 1 > displayedQuestions.size - 1) {
                Toast.makeText(context, "No more questions", Toast.LENGTH_SHORT).show()
            } else {
                navigateToNextQuestion(navController, questionIndex, filter)
            }
            viewModel.resetDelight()
        }
    }

    DisposableEffect(Unit) {
        if (!isMuted) mediaPlayer.start()
        onDispose { mediaPlayer.stop() }
    }

    val showDelight by viewModel.showDelight.collectAsState()
    val recentStats = viewModel.getLastFiveStats()
    LaunchedEffect(question) {
        viewModel.checkAndShowDelight()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                LinearProgressIndicator(
                    progress = timerProgress.value,
                    color = Color.Green,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                QuestionAndAnswerLayout(
                    question = question,
                    context = context,
                    selectedAnswerCallback = { index ->
                        selectedAnswer.value = index
                    },
                    navigateCallback = {
                        viewModel.onQuestionAnswered(
                            isCorrect = selectedAnswer.value == question?.correctOption
                        )
                        navigateToExplanationScreen(
                            navController,
                            questionIndex,
                            selectedAnswer.value,
                            filter
                        )
                    }
                )
            }

            item {
                BookMarkButton(viewModel, question)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Button(
                    onClick = {
                        isMuted = !isMuted
                        if (isMuted) mediaPlayer.pause() else mediaPlayer.start()
                    }
                ) {
                    Text(if (isMuted) "Unmute" else "Mute")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        DelightOverlay(
            isVisible = showDelight,
            recentStats = recentStats,
            onDismiss = { viewModel.resetDelight() }
        )
    }
}

@Composable
fun BookMarkButton(viewModel: QuizViewModel, question: QuestionEntity? = null) {

    question?.let {
        Button(
            onClick = { viewModel.toggleBookmark(it) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            if (question.isBookmarked) {
                Image(
                    painter = painterResource(id = R.drawable.bookmark_fill),
                    contentDescription = "Remove Bookmark",
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.bookmark_outline),
                    contentDescription = "Bookmark",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

}


@Composable
fun QuestionAndAnswerLayout(
    question: QuestionEntity?,
    context: Context,
    selectedAnswer: Int? = null,
    isInteractive: Boolean = true,
    selectedAnswerCallback: ((index: Int) -> Unit)? = null,
    navigateCallback: (() -> Unit)? = null
) {
    // Question Text

    RenderContentBasedOnType(question?.questionType ?: "text", question?.question ?: "Loading...")

    Spacer(modifier = Modifier.height(16.dp))

    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.sound_effect)
    }

    // Answer Options
    question?.let {
        val optionsList: List<String> = Gson().fromJson(
            question.options,
            object : TypeToken<List<String>>() {}.type
        )
        optionsList.forEachIndexed { i, option ->
            val color = if (isInteractive) {
                Color.Transparent
            } else {
                when {
                    i + 1 == selectedAnswer && selectedAnswer != question.correctOption -> Color.Red
                    i + 1 == selectedAnswer && selectedAnswer == question.correctOption -> Color.Green
                    i + 1 == question.correctOption -> Color.Green
                    else -> Color.Transparent
                }
            }

            Button(
                onClick = {
                    if (isInteractive) {
                        selectedAnswerCallback?.invoke(i + 1)
                        playSoundEffect(mediaPlayer)
                        navigateCallback?.invoke()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                border = BorderStroke(10.dp, color)
            ) {
                Text(text = option)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}


fun navigateToExplanationScreen(
    navController: NavController,
    questionIndex: Int,
    selectedAnswer: Int,
    filter: String
) {
    navController.navigate("answerExplanation?filter=$filter/$questionIndex/$selectedAnswer")
}

fun navigateToNextQuestion(
    navController: NavController,
    questionIndex: Int,
    filter: String
) {
    navController.navigate("nextQuestion?filter=$filter/${questionIndex + 1}")
}

fun playSoundEffect(mediaPlayer: MediaPlayer) {
    mediaPlayer.start()
    mediaPlayer.setOnCompletionListener { it.release() }
}

@Composable
fun DelightOverlay(isVisible: Boolean, recentStats: String, onDismiss: () -> Unit) {
    if (isVisible) {
        // Load the Lottie animation composition
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.celebration))
        val progress by animateLottieCompositionAsState(composition)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Congrats!", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))

                // Display the Lottie animation
                LottieAnimation(
                    composition = composition,
                    progress = {
                        progress
                    },
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = recentStats, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onDismiss) {
                    Text("Continue")
                }
            }
        }
    }
}




