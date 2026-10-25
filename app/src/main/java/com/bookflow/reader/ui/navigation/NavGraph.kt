package com.bookflow.reader.ui.navigation

import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookflow.reader.ui.screens.home.HomeScreen
import com.bookflow.reader.ui.screens.reader.ReaderScreen
import com.bookflow.reader.ui.screens.reader.ReaderViewModel
import com.bookflow.reader.ui.screens.setup.SetupScreen
import com.bookflow.reader.ui.screens.splash.SplashScreen

@Composable
fun BookFlowNavGraph(initialPdfUri: Uri? = null) {
    val navController = rememberNavController()
    
    LaunchedEffect(initialPdfUri) {
        if (initialPdfUri != null) {
            val encodedUri = Uri.encode(initialPdfUri.toString())
            navController.navigate("reader/$encodedUri/0") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(200))
        }
    ) {
        composable(
            route = "splash",
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            SplashScreen(
                onFinished = {
                    if (initialPdfUri == null) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = "home",
            enterTransition = { fadeIn(tween(350)) }
        ) {
            HomeScreen(navController = navController)
        }
        composable(
            route = "setup/{bookUri}",
            arguments = listOf(navArgument("bookUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookUri = backStackEntry.arguments?.getString("bookUri") ?: ""
            SetupScreen(navController = navController, bookUri = bookUri)
        }
        composable(
            route = "reader/{bookUri}/{pageOffset}?highlight={highlight}",
            arguments = listOf(
                navArgument("bookUri") { type = NavType.StringType },
                navArgument("pageOffset") { type = NavType.IntType },
                navArgument("highlight") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val bookUri = backStackEntry.arguments?.getString("bookUri") ?: ""
            val pageOffset = backStackEntry.arguments?.getInt("pageOffset") ?: 0
            val highlight = backStackEntry.arguments?.getString("highlight")
            val context = LocalContext.current
            val viewModel: ReaderViewModel = viewModel(
                factory = ReaderViewModel.factory(context.applicationContext as Application)
            )
            ReaderScreen(
                bookUri = bookUri,
                pageOffset = pageOffset,
                navController = navController,
                viewModel = viewModel,
                highlightCrop = highlight
            )
        }
    }
}
