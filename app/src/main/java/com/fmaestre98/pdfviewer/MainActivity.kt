package com.fmaestre98.pdfviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fmaestre98.pdfviewer.ui.navigation.AppNavigation
import com.fmaestre98.pdfviewer.ui.screens.home.HomeRoot
import com.fmaestre98.pdfviewer.ui.screens.reader.PdfReaderRoot
import com.fmaestre98.pdfviewer.ui.theme.PdfViewerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PdfViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = AppNavigation.ROUTE_HOME
                    ) {
                        composable(route = AppNavigation.ROUTE_HOME) {
                            HomeRoot(
                                onNavigateToReader = { uri ->
                                    navController.navigate(AppNavigation.createReaderRoute(uri))
                                }
                            )
                        }
                        composable(
                            route = AppNavigation.ROUTE_READER,
                            arguments = listOf(
                                navArgument("encodedUri") { type = NavType.StringType }
                            )
                        ) {
                            PdfReaderRoot(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}