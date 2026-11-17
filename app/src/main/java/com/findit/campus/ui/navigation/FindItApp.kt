package com.findit.campus.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.findit.campus.ui.auth.ForgotPasswordScreen
import com.findit.campus.ui.auth.LoginScreen
import com.findit.campus.ui.auth.RegisterScreen
import com.findit.campus.ui.auth.SessionViewModel
import com.findit.campus.ui.auth.WelcomeScreen
import com.findit.campus.ui.chat.ChatScreen
import com.findit.campus.ui.chat.InboxScreen
import com.findit.campus.ui.detail.DetailScreen
import com.findit.campus.ui.detail.DetailViewModel
import com.findit.campus.ui.home.DiscoverScreen
import com.findit.campus.ui.home.HomeViewModel
import com.findit.campus.ui.map.MapScreen
import com.findit.campus.ui.map.MapViewModel
import com.findit.campus.ui.mine.MineScreen
import com.findit.campus.ui.mine.MineViewModel
import com.findit.campus.ui.post.PostEvent
import com.findit.campus.ui.post.PostScreen
import com.findit.campus.ui.post.PostViewModel
import com.findit.campus.ui.scan.ScanScreen
import com.findit.campus.ui.scan.ScanViewModel

private object Routes {
    const val DISCOVER = "discover"
    const val MAP = "map"
    const val SCAN = "scan"
    const val MINE = "mine"
    const val INBOX = "inbox"
    const val CHAT = "chat/{itemId}"
    const val POST = "post?prefill={prefill}"
    const val DETAIL = "detail/{itemId}"

    fun post(prefill: String = "") = "post?prefill=${Uri.encode(prefill.take(240))}"
    fun detail(id: Long) = "detail/$id"
    fun chat(id: Long) = "chat/$id"
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab(Routes.DISCOVER, "Discover", Icons.Outlined.Explore, Icons.Rounded.Explore),
    Tab(Routes.MAP, "Map", Icons.Outlined.Map, Icons.Rounded.Map),
    Tab(Routes.INBOX, "Chat", Icons.Outlined.Chat, Icons.Rounded.Chat),
    Tab(Routes.SCAN, "Scan", Icons.Outlined.DocumentScanner, Icons.Rounded.DocumentScanner),
    Tab(Routes.MINE, "Mine", Icons.Outlined.Inventory2, Icons.Rounded.Inventory2),
)

@Composable
fun FindItApp(widthClass: WindowWidthSizeClass) {
    val session: SessionViewModel = viewModel()
    val account by session.account.collectAsStateWithLifecycle()
    if (account == null) {
        AuthFlow(session = session)
        return
    }
    val signedIn = account!!
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBar = route == Routes.DISCOVER || route == Routes.MAP || route == Routes.SCAN || route == Routes.MINE || route == Routes.INBOX
    val expanded = widthClass == WindowWidthSizeClass.Expanded
    val homeViewModel: HomeViewModel = viewModel()
    LaunchedEffect(backStack) {
        val message = backStack?.savedStateHandle?.remove<String>("notice")
        if (!message.isNullOrBlank()) snackbar.showSnackbar(message)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = route == Routes.DISCOVER || route == Routes.MINE,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.post()) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Report") },
                )
            }
        },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (route == tab.route) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DISCOVER,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable(Routes.DISCOVER) {
                val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                DiscoverScreen(
                    state = state,
                    expanded = expanded,
                    onQuery = homeViewModel::onQuery,
                    onType = homeViewModel::onType,
                    onOpenOnly = homeViewModel::onOpenOnly,
                    onCategory = homeViewModel::onCategory,
                    onClear = homeViewModel::clearFilters,
                    onOpenItem = { id ->
                        homeViewModel.select(id)
                        if (!expanded) navController.navigate(Routes.detail(id))
                    },
                    onSelect = homeViewModel::select,
                    onReunite = homeViewModel::reunite,
                    onDelete = homeViewModel::delete,
                    displayName = signedIn.displayName,
                    campusName = signedIn.campusName,
                    viewerId = signedIn.id,
                    onFound = { id -> navController.navigate(Routes.chat(id)) },
                )
            }
            composable(Routes.MAP) {
                val model: MapViewModel = viewModel()
                val state by model.uiState.collectAsStateWithLifecycle()
                MapScreen(
                    state = state,
                    onFocus = model::onFocus,
                    onType = model::onType,
                    onSelect = model::select,
                    onClear = model::clearSelection,
                    onOpen = { navController.navigate(Routes.detail(it)) },
                    onTracking = model::setTracking,
                    onPermission = model::setPermission,
                )
            }
            composable(Routes.SCAN) {
                val model: ScanViewModel = viewModel()
                val state by model.uiState.collectAsStateWithLifecycle()
                ScanScreen(
                    state = state,
                    onText = model::onText,
                    onPhoto = model::onPhoto,
                    onOpen = { navController.navigate(Routes.detail(it)) },
                    onUseText = { navController.navigate(Routes.post(it)) },
                )
            }
            composable(Routes.INBOX) {
                InboxScreen(onOpen = { navController.navigate(Routes.chat(it)) })
            }
            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
            ) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MINE) {
                val model: MineViewModel = viewModel()
                val state by model.uiState.collectAsStateWithLifecycle()
                MineScreen(
                    state = state,
                    onOpen = { navController.navigate(Routes.detail(it)) },
                    accountName = signedIn.displayName,
                    campusName = signedIn.campusName,
                    onLogout = session::logout,
                )
            }
            composable(
                route = Routes.POST,
                arguments = listOf(
                    navArgument("prefill") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                val model: PostViewModel = viewModel()
                val state by model.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(model) {
                    model.eventsFlow.collect { event ->
                        if (event is PostEvent.Saved) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("notice", "Report posted")
                            navController.popBackStack()
                        }
                    }
                }
                PostScreen(
                    state = state,
                    onType = model::onType,
                    onTitle = model::onTitle,
                    onDescription = model::onDescription,
                    onCategory = model::onCategory,
                    onWhen = model::onWhen,
                    onContact = model::onContact,
                    onPlaceName = model::onPlaceName,
                    onPrivateDetail = model::onPrivateDetail,
                    onUseLocation = model::useMyLocation,
                    onPhoto = model::onPhoto,
                    onClearPhoto = model::clearPhoto,
                    onSave = model::save,
                    onDismissDuplicate = model::dismissDuplicate,
                    onViewDuplicate = { id -> navController.navigate(Routes.detail(id)) },
                    onConsumeMessage = model::consumeMessage,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
            ) {
                val model: DetailViewModel = viewModel()
                val state by model.uiState.collectAsStateWithLifecycle()
                DetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onOpenMatch = { navController.navigate(Routes.detail(it)) },
                    onReunite = model::reunite,
                    onDelete = {
                        model.delete {
                            navController.previousBackStackEntry?.savedStateHandle?.set("notice", "Report removed")
                            navController.popBackStack()
                        }
                    },
                    viewerId = signedIn.id,
                    onReachOut = { navController.navigate(Routes.chat(state.item?.id ?: return@DetailScreen)) },
                )
            }
        }
    }
}

@Composable
private fun AuthFlow(session: SessionViewModel) {
    val navController = rememberNavController()
    var error by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var created by remember { mutableStateOf(false) }
    NavHost(navController = navController, startDestination = "welcome", modifier = Modifier.statusBarsPadding()) {
        composable("welcome") {
            WelcomeScreen(
                onCreate = { navController.navigate("register") },
                onLogin = { navController.navigate("login") },
            )
        }
        composable("register") {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onLogin = { navController.navigate("login") },
                error = error,
                onSubmit = { name, email, password, confirm, campus ->
                    session.register(name, email, password, confirm, campus) { message ->
                        if (message == null) {
                            error = null
                            created = true
                            navController.navigate("login") {
                                popUpTo("welcome")
                            }
                        } else {
                            error = message
                        }
                    }
                },
            )
        }
        composable("login") {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onCreate = { navController.navigate("register") },
                onForgot = { navController.navigate("forgot") },
                error = error,
                notice = if (created) "Account created. Log in to open your dashboard." else null,
                onSubmit = { email, password -> session.login(email, password) { error = it } },
            )
        }
        composable("forgot") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}
