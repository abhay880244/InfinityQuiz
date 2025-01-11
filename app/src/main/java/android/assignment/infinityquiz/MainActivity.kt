package android.assignment.infinityquiz

import android.assignment.infinityquiz.composable.AnswerExplanationScreen
import android.assignment.infinityquiz.composable.HomeScreen
import android.assignment.infinityquiz.composable.QuestionScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import android.assignment.infinityquiz.ui.theme.InfinityQuizTheme
import android.assignment.infinityquiz.viewmodel.QuizViewModel
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InfinityQuizTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: QuizViewModel = hiltViewModel()  // Scope ViewModel to the NavController
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("quiz?filter={filter}") { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter") ?: "all"
            QuestionScreen(navController = navController, viewModel = viewModel, filter = filter)
            BackHandling(viewModel, navController)
        }
        composable("nextQuestion?filter={filter}/{questionIndex}") { backStackEntry ->
            val questionIndex = backStackEntry.arguments?.getString("questionIndex")?.toInt() ?: 0
            val filter = backStackEntry.arguments?.getString("filter") ?: "all"
            QuestionScreen(
                navController = navController,
                filter = filter,
                viewModel = viewModel,
                questionIndex = questionIndex
            )
            BackHandling(viewModel, navController)
        }
        composable("answerExplanation?filter={filter}/{questionIndex}/{selectedAnswer}") { backStackEntry ->
            val questionIndex = backStackEntry.arguments?.getString("questionIndex")?.toInt() ?: 0
            val selectedAnswer = backStackEntry.arguments?.getString("selectedAnswer")?.toInt() ?: 0
            val filter = backStackEntry.arguments?.getString("filter") ?: "all"
            AnswerExplanationScreen(questionIndex, selectedAnswer, navController, filter, viewModel)
            BackHandling(viewModel, navController)
        }
    }
}

@Composable
fun BackHandling(viewModel: QuizViewModel, navController: NavHostController) {
    BackHandler {
        viewModel.resetDelight()
        navController.navigate("home") {
            popUpTo("home") { inclusive = true }
        }
    }
}

