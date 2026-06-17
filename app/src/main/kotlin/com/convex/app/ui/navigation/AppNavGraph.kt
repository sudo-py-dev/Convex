package com.convex.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.convex.app.ui.advanced.advancedScreen
import com.convex.app.ui.history.historyScreen
import com.convex.app.ui.home.categoryDetailScreen
import com.convex.app.ui.home.homeScreen
import com.convex.app.ui.operation.operationScreen
import com.convex.app.ui.settings.settingsScreen

@Composable
fun appNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            homeScreen(
                onCategoryClick = { categoryId ->
                    navController.navigate(Routes.categoryDetail(categoryId))
                },
                onRecentClick = { categoryId, operationId ->
                    navController.navigate(Routes.operationForm(categoryId, operationId))
                }
            )
        }

        composable(Routes.CATEGORY_DETAIL) { backStack ->
            val categoryId = backStack.arguments?.getString("categoryId") ?: ""
            categoryDetailScreen(
                categoryId = categoryId,
                onOperationClick = { operationId ->
                    navController.navigate(Routes.operationForm(categoryId, operationId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.OPERATION_FORM) {
            operationScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY) {
            historyScreen()
        }

        composable(Routes.ADVANCED) {
            advancedScreen()
        }

        composable(Routes.SETTINGS) {
            settingsScreen()
        }
    }
}
