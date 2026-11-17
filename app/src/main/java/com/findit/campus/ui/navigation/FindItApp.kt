package com.findit.campus.ui.navigation

import com.findit.campus.model.ItemType

import android.net.Uri
import com.findit.campus.notifications.AlertDestination
import com.findit.campus.ui.components.*
import com.findit.campus.ui.chat.InboxViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.findit.campus.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
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
import com.findit.campus.ui.auth.GoogleCampusScreen
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
    const val ALERTS = "alerts"
    const val CHAT = "chat/{itemId}"
    const val CONVERSATION = "conversation/{threadId}"
    const val POST = "post?prefill={prefill}&kind={kind}"
    const val DETAIL = "detail/{itemId}"

    fun post(prefill: String = "", kind: String = "LOST") = "post?prefill=${Uri.encode(prefill.take(240))}&kind=$kind"
    fun conversation(id: Long) = "conversation/$id"
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
    Tab(Routes.DISCOVER, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(Routes.MAP, "Map", Icons.Outlined.Map, Icons.Rounded.Map),
    Tab(Routes.INBOX, "Messages", Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Rounded.Chat),
    Tab(Routes.SCAN, "Scan", Icons.Outlined.DocumentScanner, Icons.Rounded.DocumentScanner),
    Tab(Routes.MINE, "Profile", Icons.Outlined.PersonOutline, Icons.Rounded.Person),
)

@Composable
fun FindItApp(widthClass: WindowWidthSizeClass, alert: AlertDestination? = null, onAlertConsumed: () -> Unit = {}) {
    val feedback = rememberToastFeedback()
    CompositionLocalProvider(LocalFeedback provides feedback) {
        FindItContent(widthClass, alert, onAlertConsumed)
    }
}

@Composable
private fun FindItContent(widthClass: WindowWidthSizeClass, alert: AlertDestination?, onAlertConsumed: () -> Unit) {
    val session: SessionViewModel = viewModel()
    val sessionState by session.state.collectAsStateWithLifecycle()
    if (sessionState.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Brand()
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        }
        return
    }
    val account = sessionState.account
    if (account == null) {
        AuthFlow(session = session)
        return
    }
    val signedIn = account
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBar = route == Routes.DISCOVER || route == Routes.MAP || route == Routes.SCAN || route == Routes.MINE || route == Routes.INBOX || route == Routes.ALERTS
    val wide = widthClass != WindowWidthSizeClass.Compact
    val inbox: InboxViewModel = viewModel()
    val notifications by inbox.notifications.collectAsStateWithLifecycle()
    val unread = notifications.count { !it.read }
    val feedback = LocalFeedback.current
    val homeViewModel: HomeViewModel = viewModel()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val sync by homeViewModel.syncState.collectAsStateWithLifecycle()
    val pending by homeViewModel.pending.collectAsStateWithLifecycle()
    val scrollRequest by homeViewModel.refresh.collectAsStateWithLifecycle()
    var connectOpen by remember { mutableStateOf(false) }
    var connectPassword by remember { mutableStateOf("") }
    var connectError by remember { mutableStateOf<String?>(null) }
    val connecting by session.busy.collectAsStateWithLifecycle()
    fun openTab(target: String) {
        if (target == Routes.DISCOVER) homeViewModel.openHome()
        navController.openCampusTab(target, Routes.DISCOVER)
    }
    LaunchedEffect(alert, signedIn.id) {
        alert?.let { destination ->
            if (destination.accountId == signedIn.id) navController.navigate(
                destination.threadId?.let(Routes::conversation) ?: destination.itemId?.let(Routes::detail) ?: Routes.ALERTS
            ) { launchSingleTop = true }
            else feedback("This update belongs to a different account. Open Messages after signing in to that account.")
            onAlertConsumed()
        }
    }
    val expanded = widthClass == WindowWidthSizeClass.Expanded
    LaunchedEffect(backStack) {
        val message = backStack?.savedStateHandle?.remove<String>("notice")
        if (!message.isNullOrBlank()) snackbar.showSnackbar(message)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) { FeedbackSnackbar(it) } },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !wide && (route == Routes.MINE || (route == Routes.DISCOVER &&
                    homeState.items.any { it.type == (homeState.type ?: ItemType.LOST) })),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.post()) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Report") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                )
            }
        },
        bottomBar = {
            if (showBar && !wide) {
              Surface(Modifier.padding(horizontal = 12.dp).padding(top = 6.dp, bottom = 8.dp).navigationBarsPadding(),
                  shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)), shadowElevation = 4.dp) {
                CampusNavigationBar(route, unread, ::openTab)
              }
            }
        },
    ) { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
            if (showBar && wide) NavigationRail(header = {
                Image(painterResource(R.drawable.cutout_2_d511eb0ee341), "FindIt", Modifier.padding(12.dp).size(34.dp))
                FloatingActionButton({ navController.navigate(Routes.post()) }, Modifier.padding(8.dp)) {
                    Icon(Icons.Filled.Add, "Report a lost item")
                }
            }) {
                tabs.forEach { tab -> NavigationRailItem(
                    selected = route == tab.route || (tab.route == Routes.INBOX && route == Routes.ALERTS),
                    onClick = { openTab(tab.route) }, icon = { TabIcon(tab, route, unread) }, label = { Text(tab.label) },
                ) }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = if (route == Routes.DISCOVER || route == Routes.MAP) 1200.dp else 680.dp).fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.DISCOVER,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(240)) { it / 12 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { it / 12 } },
        ) {
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    state = homeState,
                    onPostLost = { navController.navigate(Routes.post()) },
                    expanded = expanded,
                    onQuery = homeViewModel::onQuery,
                    onType = homeViewModel::onType,
                    onOpenOnly = homeViewModel::onOpenOnly,
                    onCategory = homeViewModel::onCategory,
                    onClear = homeViewModel::clearFilters,
                    onOpenItem = { id ->
                        homeViewModel.select(id)
                        navController.navigate(Routes.detail(id))
                    },
                    onSelect = homeViewModel::select,
                    onReunite = homeViewModel::reunite,
                    onDelete = homeViewModel::delete,
                    displayName = signedIn.displayName,
                    campusName = signedIn.campusName,
                    viewerId = signedIn.id,
                    onFound = { id -> navController.navigate(Routes.chat(id)) },
                    onInbox = { navController.navigate(Routes.ALERTS) },
                    unreadCount = unread,
                    onProfile = { openTab(Routes.MINE) },
                    onPostFound = { navController.navigate(Routes.post(kind = "FOUND")) },
                    scrollRequest = scrollRequest,
                    syncMessage = when {
                        signedIn.remoteUid == null -> "Your account is saved on this phone. Connect to share reports and message other students."
                        sync.error != null -> sync.error
                        pending.isNotEmpty() -> "${pending.size} changes waiting to upload. Keep your connection on."
                        !sync.connected -> "Connecting to your campus… Saved reports remain available."
                        else -> null
                    },
                    onConnect = if (signedIn.remoteUid == null || sync.error?.startsWith("Sign in online") == true) ({ connectOpen = true }) else null,
                    onScan = { openTab(Routes.SCAN) },
                    onMap = { openTab(Routes.MAP) },
                    onMessages = { openTab(Routes.INBOX) },
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
                    onToggleTrail = model::toggleTrail,
                    onClearTrail = model::clearTrail,
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
            composable(Routes.ALERTS) {
                InboxScreen(onOpen = { navController.navigate(Routes.conversation(it)) },
                    onOpenReport = { navController.navigate(Routes.detail(it)) }, startAlerts = true)
            }
            composable(Routes.INBOX) {
                InboxScreen(onOpen = { navController.navigate(Routes.conversation(it)) }, onOpenReport = { navController.navigate(Routes.detail(it)) })
            }
            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
            ) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CONVERSATION, arguments = listOf(navArgument("threadId") { type = NavType.LongType })) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MINE) {
                val model: MineViewModel = viewModel()
                val state by model.uiState.collectAsStateWithLifecycle()
                MineScreen(
                    state = state,
                    onOpen = { navController.navigate(Routes.detail(it)) },
                    accountName = signedIn.displayName,
                    email = signedIn.email,
                    campusName = signedIn.campusName,
                    onLogout = session::logout,
                    onScan = { openTab(Routes.SCAN) },
                    onMap = { openTab(Routes.MAP) },
                    onMessages = { openTab(Routes.INBOX) },
                    onResetPassword = { session.resetPassword(signedIn.email, feedback) },
                )
            }
            composable(
                route = Routes.POST,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType; defaultValue = "LOST" },
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
                            homeViewModel.openHome(model.uiState.value.form.type)
                            navController.openCampusTab(Routes.DISCOVER, Routes.DISCOVER)
                            feedback(if (signedIn.remoteUid == null) "Saved on this phone. Connect your account to share it."
                                else "Report saved. Uploading to your campus…")
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
                    onReadPhoto = model::readPhoto,
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
                    onReachOut = {
                        val item = state.item ?: return@DetailScreen
                        navController.navigate(if (item.ownerId == signedIn.id) Routes.INBOX else Routes.chat(item.id))
                    },
                )
            }
        }
            }
        }
        }
    }
    if (connectOpen) AlertDialog(onDismissRequest = { if (!connecting) { connectOpen = false; connectPassword = "" } },
        title = { Text("Connect to your campus") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Use the password for ${signedIn.email}. Your saved reports and conversations stay with your account.")
                OutlinedTextField(connectPassword, { connectPassword = it }, label = { Text("Account password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true, enabled = !connecting)
                connectError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (connecting) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }, confirmButton = { TextButton({ session.connect(connectPassword) { error ->
            connectError = error
            if (error == null) { connectOpen = false; connectPassword = ""; homeViewModel.openHome(); feedback("Connected. Your reports are uploading to campus.") }
        } }, enabled = connectPassword.isNotBlank() && !connecting) { Text("Connect") } },
        dismissButton = { TextButton({ connectOpen = false; connectPassword = "" }, enabled = !connecting) { Text("Later") } })
}

internal fun androidx.navigation.NavHostController.openCampusTab(target: String, home: String) {
    if (target == home && popBackStack(home, false)) return
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = target != home }
        launchSingleTop = true
        restoreState = target != home
    }
}

@Composable
internal fun CampusNavigationBar(route: String, unread: Int = 0, onTab: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0), modifier = Modifier.heightIn(min = 80.dp)) {
        tabs.forEach { tab ->
            NavigationBarItem(selected = route == tab.route || (tab.route == Routes.INBOX && route == Routes.ALERTS),
                onClick = { onTab(tab.route) }, icon = { TabIcon(tab, route, unread) }, label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
private fun TabIcon(tab: Tab, route: String, unread: Int) {
    val selected = route == tab.route || (tab.route == Routes.INBOX && route == Routes.ALERTS)
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, tween(220), label = "tab selection")
    val iconModifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        .semantics { if (tab.route == Routes.INBOX) stateDescription = "$unread unread alerts" }
    BadgedBox(badge = { if (tab.route == Routes.INBOX && unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) {
        Icon(if (selected) tab.selectedIcon else tab.icon, tab.label, iconModifier.size(24.dp))
    }
}

@Composable
private fun AuthFlow(session: SessionViewModel) {
    val navController = rememberNavController()
    var error by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val busy by session.busy.collectAsStateWithLifecycle()
    var created by remember { mutableStateOf(false) }
    val feedback = LocalFeedback.current
    val draft by session.googleDraft.collectAsStateWithLifecycle()
    val google: (String) -> Unit = { token -> session.google(token) { error = it } }
    if (draft != null) {
        ReadingPane(Modifier.statusBarsPadding()) {
            GoogleCampusScreen(draft!!, busy, error, { session.cancelGoogle(); error = null }) { name, campus ->
                session.completeGoogle(name, campus) { error = it; if (it == null) feedback("Welcome to your campus") }
            }
        }
        return
    }
    ReadingPane {
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
                busy = busy,
                onGoogleToken = google,
                onGoogleError = { error = it },
                onSubmit = { name, email, password, confirm, campus ->
                    session.register(name, email, password, confirm, campus) { message ->
                        if (message == null) {
                            error = null
                            created = true
                            feedback("Account created. Log in to continue.")
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
                busy = busy,
                onGoogleToken = google,
                onGoogleError = { error = it },
                notice = if (created) "Account created. Log in to open your dashboard." else null,
                onSubmit = { email, password -> session.login(email, password) { error = it; if (it == null) feedback("Welcome back") } },
            )
        }
        composable("forgot") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
    }
    }
}
