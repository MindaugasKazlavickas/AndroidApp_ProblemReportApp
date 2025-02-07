package com.example.problemreportapp

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.problemreportapp.ui.MainScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                navController = navController
            )
        }
        composable(
            "submission?imageUri={imageUri}&coordinates={coordinates}&message={message}",
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("coordinates") { type = NavType.StringType },
                navArgument("message") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            val coordinatesString = backStackEntry.arguments?.getString("coordinates")
            val message = backStackEntry.arguments?.getString("message")

            // Convert the imageUri string to Uri
            val imageUri = imageUriString?.let { Uri.parse(it) }

            // Parse coordinates
            val coordinates = coordinatesString?.let {
                val parts = it.split(",")
                Pair(parts[0].toDoubleOrNull() ?: 0.0, parts[1].toDoubleOrNull() ?: 0.0)
            }

            // Pass the Uri to SubmissionScreen
            SubmissionScreen(navController, imageUri, coordinates, message ?: "")
        }
    }
}