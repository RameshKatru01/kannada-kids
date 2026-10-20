package com.example.ui.navigation

import com.example.platform.*

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthState
import com.example.ui.auth.LoginScreen
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.example.ui.screens.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel { AuthViewModel() }) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "login"
    val context = LocalContext.current
    
    val authState by authViewModel.authState.collectAsState()
    val currentEmail = (authState as? AuthState.Authenticated)?.email ?: ""
    val isAdmin = com.example.ui.data.UserManager.isAdmin(currentEmail)
    val startDestination = "login"

    Scaffold(
        containerColor = Color(0xFFE1F5FE),
        bottomBar = {
            if (currentRoute != "login" && !currentRoute.startsWith("letter_flow") && !currentRoute.startsWith("video_player")) {
                BentoBottomNav(
                    currentRoute = currentRoute,
                    isAdmin = isAdmin,
                    onNavigate = { route ->
                        try {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Nav error", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        },

    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            
            composable("login") {
                LoginScreen(
                    onNavigateToHome = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            val signOutAction: () -> Unit = {
                authViewModel.signOut(context)
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }

            composable("home") {
                HomeScreen(
                    onNavigateToVowels = { navController.navigate("list/vowels") },
                    onNavigateToConsonants = { navController.navigate("list/consonants") },
                    onNavigateToGunninthakshari = { navController.navigate("guninthakshari") }, // Kept for map navigation
                    onNavigateToNumbers = { navController.navigate("list/numbers") },
                    onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                    onNavigateToProfile = { navController.navigate("profile") },
                    onSignOut = signOutAction
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    authViewModel = authViewModel,
                    onSignOut = signOutAction
                )
            }

            composable("admin") {
                AdministrationScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onSignOut = signOutAction
                )
            }

            composable("reference") {
                ReferenceScreen(onBack = { navController.popBackStack() })
            }
            composable("profile") {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToBadges = { navController.navigate("badges") },
                    onNavigateToAdmin = { navController.navigate("admin") },
                    onSignOut = signOutAction,
                    authViewModel = authViewModel
                )
            }
            composable("badges") {
                BadgesScreen(onBack = { navController.popBackStack() }, authViewModel = authViewModel)
            }
            composable("leaderboard") {
                LeaderboardScreen(onBack = { navController.popBackStack() })
            }

            composable(
                "list/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.read { getStringOrNull("type") } ?: "vowels"
                LetterListScreen(
                    type = type,
                    onBack = { navController.popBackStack() },
                    onNavigateToGameFlow = { flowType, index ->  
                        navController.navigate("letter_flow/$flowType/$index")
                    }
                )
            }

            composable(
                "letter_flow/{type}/{index}?char={char}&translit={translit}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("index") { type = NavType.IntType },
                    navArgument("char") { type = NavType.StringType; nullable = true },
                    navArgument("translit") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.read { getStringOrNull("type") } ?: "vowels"
                val index = backStackEntry.arguments?.read { getIntOrNull("index") } ?: 0
                val char = backStackEntry.arguments?.read { getStringOrNull("char") } ?: ""
                val translit = backStackEntry.arguments?.read { getStringOrNull("translit") } ?: ""
                LetterGameFlowScreen(
                    type = type,
                    index = index,
                    char = char,
                    translit = translit,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() }
                )
            }

            composable("guninthakshari") {
                GameScreen(
                    onNavigateToGameFlow = { char, translit, index -> navController.navigate("letter_flow/guninthakshari/$index?char=${Uri.encode(char)}&translit=${Uri.encode(translit)}") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("games") {
                GamesListScreen(
                    onNavigateToGame = { gameIndex -> navController.navigate("game/$gameIndex") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "game/{gameIndex}",
                arguments = listOf(navArgument("gameIndex") { type = NavType.IntType })
            ) { backStackEntry ->
                val gameIndex = backStackEntry.arguments?.read { getIntOrNull("gameIndex") } ?: 1
                if (gameIndex == 6) {
                    LetterRunnerScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 7) {
                    VowelMapNavigatorScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 8) {
                    ScrambledWordsScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 9) {
                    WordSearchScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 10) {
                    TraceLetterScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 11) {
                    MatchLetterScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 12) {
                    MusicalBalloonPopScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 13) {
                    BannaGaluGameScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 15) {
                    BuguriSpinScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 16) {
                    ConstellationPathGameScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 19) {
                    ListenAndChooseScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 20) {
                    SwaraAdventureScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 22) {
                    PaintTheMonsterScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex == 21) {
                    LetterTrainScreen(onBack = { navController.popBackStack() })
                } else if (gameIndex in 1..4) {
                    // Feed the Monster replaces the four Path Builder games (1 vowels, 2 consonants, 3 guninthakshari, 4 numbers)
                    FeedTheMonsterScreen(mode = gameIndex, onBack = { navController.popBackStack() })
                } else {
                    PathBuilderScreen(gameType = gameIndex, onBack = { navController.popBackStack() })
                }
            }

            composable("numbers") {
                NumbersScreen(
                    onBack = { navController.popBackStack() }
                )
            }

                        composable("fun_videos") {
                VideoFoldersScreen(
                    title = "Fun Videos",
                    folders = listOf("Vowels", "Consonants", "Birds", "Animals", "Colours", "Numbers", "Letter Quiz", "Picture Quiz", "Number Quiz", "How Many?", "Class Room", "Play Time", "Getting ready to school", "shopping mall", "Hospital", "Vacation"),
                    onFolderClick = { folderName -> 
                        navController.navigate("video_player/${Uri.encode(folderName)}")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                "flashcards/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.read { getStringOrNull("category") } ?: "Birds"
                com.example.ui.screens.FlashcardsScreen(
                    category = category,
                    onBack = { navController.popBackStack() }
                )
            }


            composable("class_videos") {
                VideoFoldersScreen(
                    title = "Class Videos",
                    folders = listOf("class 1", "class 2", "class 3", "Class 4", "Class 5"),
                    onFolderClick = { folderName -> navController.navigate("video_player/${Uri.encode(folderName)}") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "video_player/{folderName}",
                arguments = listOf(navArgument("folderName") { type = NavType.StringType })
            ) { backStackEntry ->
                val folderName = backStackEntry.arguments?.read { getStringOrNull("folderName") } ?: "Video"
                VideoScreen(
                    title = folderName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
