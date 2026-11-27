package com.example.ui.classroom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read

internal const val CLASS_HUB = "classroom"
internal const val CLASS_HOMEWORK = "classroom/homework"
internal const val CLASS_TRACE = "classroom/trace"

/**
 * All classroom screens. Added to the app's NavHost by one line in AppNavigation
 * (only when FeatureFlags.CLASSROOM is on).
 */
fun NavGraphBuilder.classroomRoutes(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }

    composable(CLASS_HUB) {
        ClassroomHubScreen(
            onBack = back,
            onOpenHomework = { nav.navigate(CLASS_HOMEWORK) },
            onOpenClass = { code -> nav.navigate("classroom/class/$code") }
        )
    }
    composable(CLASS_HOMEWORK) {
        StudentHomeworkScreen(
            onBack = back,
            onOpenRoute = { route -> nav.navigate(route) },
            onOpenClassroom = { nav.navigate(CLASS_HUB) }
        )
    }
    composable(CLASS_TRACE) {
        HomeworkTraceScreen(onBack = back)
    }
    composable(
        "classroom/class/{code}",
        arguments = listOf(navArgument("code") { type = NavType.StringType })
    ) { entry ->
        val code = entry.arguments?.read { getStringOrNull("code") } ?: ""
        TeacherClassScreen(
            code = code,
            onBack = back,
            onNewHomework = { nav.navigate("classroom/new/$code") },
            onOpenHomework = { id -> nav.navigate("classroom/progress/$code/$id") }
        )
    }
    composable(
        "classroom/new/{code}",
        arguments = listOf(navArgument("code") { type = NavType.StringType })
    ) { entry ->
        val code = entry.arguments?.read { getStringOrNull("code") } ?: ""
        NewHomeworkScreen(code = code, onBack = back, onSaved = back)
    }
    composable(
        "classroom/progress/{code}/{hw}",
        arguments = listOf(
            navArgument("code") { type = NavType.StringType },
            navArgument("hw") { type = NavType.StringType }
        )
    ) { entry ->
        val code = entry.arguments?.read { getStringOrNull("code") } ?: ""
        val hw = entry.arguments?.read { getStringOrNull("hw") } ?: ""
        HomeworkProgressScreen(code = code, hwId = hw, onBack = back)
    }
}

/** Home screen with the "Homework" / "Class" button floating at the bottom right. */
@Composable
fun WithHomeworkButton(nav: NavHostController, home: @Composable () -> Unit) {
    Box {
        home()
        HomeworkHomeButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            onOpenHomework = { nav.navigate(CLASS_HOMEWORK) },
            onOpenClassroom = { nav.navigate(CLASS_HUB) }
        )
    }
}
