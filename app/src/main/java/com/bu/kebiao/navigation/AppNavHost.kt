package com.bu.kebiao.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bu.kebiao.ui.components.BuBottomNav
import com.bu.kebiao.ui.components.NavDestination
import com.bu.kebiao.ui.courseedit.CourseEditDestination
import com.bu.kebiao.ui.courseedit.CourseEditScreen
import com.bu.kebiao.ui.home.HomeScreen
import com.bu.kebiao.ui.courseimport.ImportScreen
import com.bu.kebiao.ui.settings.SettingsScreen
import com.bu.kebiao.ui.theme.BuDuration
import com.bu.kebiao.ui.theme.BuEasing

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentDestination = NavDestination.entries.find { it.route == currentRoute }
    val showBottomBar = currentDestination != null

    Scaffold(
        bottomBar = {
            if (showBottomBar && currentDestination != null) {
                BuBottomNav(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 3 },
                    animationSpec = tween(BuDuration.Medium, easing = BuEasing.Emphasized)
                ) + fadeIn(animationSpec = tween(BuDuration.Medium))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(BuDuration.Medium, easing = BuEasing.Exit)
                ) + fadeOut(animationSpec = tween(BuDuration.Medium))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(BuDuration.Medium, easing = BuEasing.Emphasized)
                ) + fadeIn(animationSpec = tween(BuDuration.Medium))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 3 },
                    animationSpec = tween(BuDuration.Medium, easing = BuEasing.Exit)
                ) + fadeOut(animationSpec = tween(BuDuration.Medium))
            }
        ) {
            composable(NavDestination.Home.route) {
                HomeScreen(
                    onImportClick = { navController.navigate("import") },
                    onOpenCourseEditor = { draft ->
                        navController.navigate(CourseEditDestination.createRoute(draft))
                    }
                )
            }

            composable(NavDestination.Settings.route) {
                SettingsScreen(
                    onImportClick = { navController.navigate("import") }
                )
            }

            composable("import") {
                ImportScreen(
                    onBack = { navController.popBackStack() },
                    onImportSuccess = {
                        // Navigate back to home to show imported courses
                        navController.navigate(NavDestination.Home.route) {
                            popUpTo(NavDestination.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = CourseEditDestination.route,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("teacher") { type = NavType.StringType; defaultValue = "" },
                    navArgument("location") { type = NavType.StringType; defaultValue = "" },
                    navArgument("day") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("start") { type = NavType.IntType; defaultValue = 1 },
                    navArgument("end") { type = NavType.IntType; defaultValue = 2 },
                    navArgument("weeks") { type = NavType.StringType; defaultValue = "1-20" },
                    navArgument("color") { type = NavType.IntType; defaultValue = 0 }
                )
            ) {
                CourseEditScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
