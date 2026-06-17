package com.convex.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.convex.app.R

/** Bottom navigation destinations. */
enum class TopDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Outlined.Home),
    HISTORY("history", R.string.nav_history, Icons.Outlined.History),
    ADVANCED("advanced", R.string.nav_advanced, Icons.Outlined.Build),
    SETTINGS("settings", R.string.nav_settings, Icons.Outlined.Settings),
}

/** All named routes in the app. */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val ADVANCED = "advanced"
    const val SETTINGS = "settings"

    /** Category list screen: categoryId is the Category.id */
    fun categoryDetail(categoryId: String) = "category/$categoryId"

    const val CATEGORY_DETAIL = "category/{categoryId}"

    /** Operation form screen */
    fun operationForm(
        categoryId: String,
        operationId: String,
    ) = "operation/$categoryId/$operationId"

    const val OPERATION_FORM = "operation/{categoryId}/{operationId}"
}
