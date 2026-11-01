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

    // Opening a game goes through the locks (Gold pack / free run of the day)
    val openGame: (com.example.ui.progress.GameInfo) -> Unit = { game ->
        when (com.example.ui.data.Access.game(game)) {
            com.example.ui.data.Access.State.Open -> when {
                game.hasLevels -> navController.navigate("levels/${game.id}")
                com.example.ui.data.Access.endlessRunLeft(game.id) -> {
                    com.example.ui.data.Access.useEndlessRun(game.id)
                    navController.navigate("game/${game.id}")
                }
                else -> com.example.ui.data.Access.askGrownUp("You had today's free run of ${game.name}! Come back tomorrow, or play as much as you like with Gold.")
            }
            else -> com.example.ui.data.Access.askGrownUp("${game.name} is in the Gold pack, with lots more games.")
        }
    }
    val openGameId: (Int) -> Unit = { id -> com.example.ui.progress.GameRegistry.byId(id)?.let(openGame) }
    val openVideoFolder: (String) -> Unit = { folder ->
        if (com.example.ui.data.Access.videoFolder(folder) == com.example.ui.data.Access.State.Open) {
            navController.navigate("video_player/${Uri.encode(folder)}")
        } else {
            com.example.ui.data.Access.askGrownUp("The $folder videos are in the Gold pack.")
        }
    }

    Box(Modifier.fillMaxSize()) {
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
                    onNavigateToOttakshara = { navController.navigate("ottakshara") },
                        onNavigateToLeaderboard = { navController.navigate("leaderboard") },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onOpenLetter = { type, index -> navController.navigate("letter_flow/$type/$index") },
                        onOpenGame = openGame,
                        onSeeAllGames = { navController.navigate("games") { launchSingleTop = true } },
                        onOpenVideos = openVideoFolder,
                        onSeeAllVideos = { navController.navigate("fun_videos") { launchSingleTop = true } },
                        onSignOut = signOutAction
                    )
                }
                composable("dashboard") {
                    DashboardScreen(
                        authViewModel = authViewModel,
                        onSignOut = signOutAction,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable("rewards") {
                    RewardsScreen(onBack = { navController.popBackStack() }, authViewModel = authViewModel)
                }
                composable("parent_corner") {
                    ParentCornerScreen(
                        onBack = { navController.popBackStack() },
                        onSignOut = signOutAction,
                        onOpenPlans = { navController.navigate("paywall") },
                        onOpenLearningPath = { navController.navigate("learning_path") },
                        authViewModel = authViewModel
                    )
                }

                composable("admin") {
                    AdministrationScreen(
                        authViewModel = authViewModel,
                        onBack = { navController.popBackStack() },
                        onSignOut = signOutAction,
                        onOpenSubscriptions = { navController.navigate("admin_subs") }
                    )
                }
                composable("admin_subs") {
                    AdminSubscriptionsScreen(
                        onBack = { navController.popBackStack() },
                        onOpen = { uid -> navController.navigate("admin_sub/$uid") },
                        onRenewals = { navController.navigate("admin_renewals") },
                        onAdmins = { navController.navigate("admin_roles") }
                    )
                }
                composable("admin_roles") {
                    AdminRolesScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    "admin_sub/{uid}",
                    arguments = listOf(navArgument("uid") { type = NavType.StringType })
                ) { backStackEntry ->
                    val uid = backStackEntry.arguments?.read { getStringOrNull("uid") } ?: ""
                    AdminSubscriberScreen(uid = uid, onBack = { navController.popBackStack() })
                }
                composable("admin_renewals") {
                    AdminRenewalsScreen(
                        onBack = { navController.popBackStack() },
                        onOpen = { uid -> navController.navigate("admin_sub/$uid") }
                    )
                }
                composable(
                    "worksheets/{section}",
                    arguments = listOf(navArgument("section") { type = NavType.StringType })
                ) { backStackEntry ->
                    val section = com.example.ui.data.Worksheets.Section.fromRoute(
                        backStackEntry.arguments?.read { getStringOrNull("section") }
                    )
                    WorksheetsScreen(section = section, onBack = { navController.popBackStack() })
                }
                composable("ottakshara") {
                    OttaksharaScreen(
                        onBack = { navController.popBackStack() },
                        onOpenLesson = { i -> navController.navigate("ottakshara_lesson/$i") }
                    )
                }
                composable(
                    "ottakshara_lesson/{index}",
                    arguments = listOf(navArgument("index") { type = NavType.IntType })
                ) { backStackEntry ->
                    val index = backStackEntry.arguments?.read { getIntOrNull("index") } ?: 0
                    OttaksharaLessonScreen(index = index, onBack = { navController.popBackStack() })
                }
                composable("learning_path") {
                    LearningPathScreen(onBack = { navController.popBackStack() })
                }
                composable("paywall") {
                    PaywallScreen(onBack = { navController.popBackStack() })
                }

                composable("reference") {
                    ReferenceScreen(
                        onBack = { navController.popBackStack() },
                        onOpenLetter = { tab, index -> navController.navigate("reference_letter/$tab/$index") },
                        onOpenWorksheets = { section -> navController.navigate("worksheets/${section.route}") }
                    )
                }
                composable(
                    "reference_letter/{tab}/{index}",
                    arguments = listOf(
                        navArgument("tab") { type = NavType.IntType },
                        navArgument("index") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val tab = backStackEntry.arguments?.read { getIntOrNull("tab") } ?: 0
                    val index = backStackEntry.arguments?.read { getIntOrNull("index") } ?: 0
                    ReferenceLetterScreen(tab = tab, startIndex = index, onBack = { navController.popBackStack() })
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
                        onOpenLevels = openGameId,
                        onNavigateToGame = openGameId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    "levels/{gameIndex}",
                    arguments = listOf(navArgument("gameIndex") { type = NavType.IntType })
                ) { backStackEntry ->
                    val gameIndex = backStackEntry.arguments?.read { getIntOrNull("gameIndex") } ?: 1
                    com.example.ui.progress.LevelMapScreen(
                        gameId = gameIndex,
                        onPlayLevel = { level -> navController.navigate("game/$gameIndex?level=$level") },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    "game/{gameIndex}?level={level}",
                    arguments = listOf(
                        navArgument("gameIndex") { type = NavType.IntType },
                        navArgument("level") { type = NavType.IntType; defaultValue = 1 }
                    )
                ) { backStackEntry ->
                    val gameIndex = backStackEntry.arguments?.read { getIntOrNull("gameIndex") } ?: 1
                    val level = backStackEntry.arguments?.read { getIntOrNull("level") } ?: 1
                    val back: () -> Unit = { navController.popBackStack() }
                    when (gameIndex) {
                        in 1..4 -> FeedTheMonsterScreen(mode = gameIndex, firstLevel = level, onBack = back)
                        21 -> LetterTrainScreen(firstLevel = level, onBack = back)
                        22 -> PaintTheMonsterScreen(firstLevel = level, onBack = back)
                        6 -> LetterRunnerScreen(onBack = back)
                        23 -> LetterDashScreen(onBack = back)
                        8 -> ScrambledWordsScreen(firstLevel = level, onBack = back)
                        9 -> WordSearchScreen(firstLevel = level, onBack = back)
                        11 -> MatchLetterScreen(firstLevel = level, onBack = back)
                        12 -> MusicalBalloonPopScreen(onBack = back)
                        13 -> ColourSortScreen(firstLevel = level, onBack = back)
                        19 -> ListenAndChooseScreen(firstLevel = level, onBack = back)
                        20 -> SwaraAdventureScreen(firstLevel = level, onBack = back)
                        else -> PathBuilderScreen(gameType = gameIndex, onBack = back)
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
                        folders = listOf("Vowels", "Consonants", "Birds", "Animals", "Colours", "Fruits", "Vegetables", "Body Parts", "Family", "Numbers", "Letter Quiz", "Picture Quiz", "Number Quiz", "How Many?", "Class Room", "Play Time"),
                        onFolderClick = openVideoFolder,
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
                        onFolderClick = openVideoFolder,
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
        AccessPromptHost(onOpenPlans = { navController.navigate("paywall") { launchSingleTop = true } })
    }
}
