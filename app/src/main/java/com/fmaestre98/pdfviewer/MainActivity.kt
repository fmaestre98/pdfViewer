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

import com.fmaestre98.pdfviewer.ui.screens.annotations.AnnotationsRoot

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
                                },
                                onNavigateToAnnotations = {
                                    navController.navigate(AppNavigation.ROUTE_ANNOTATIONS)
                                }
                            )
                        }
                        composable(route = AppNavigation.ROUTE_ANNOTATIONS) {
                            AnnotationsRoot(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToReader = { uri, page ->
                                    navController.navigate(AppNavigation.createReaderRoute(uri, page))
                                }
                            )
                        }
                        composable(
                            route = AppNavigation.ROUTE_READER,
                            arguments = listOf(
                                navArgument("encodedUri") { type = NavType.StringType },
                                navArgument("initialPage") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
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