package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.UserRole
import com.example.ui.admin.*
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.MainTopAppBar
import com.example.ui.publicview.PublicEventHistoryScreen
import com.example.ui.publicview.PublicEventOverviewScreen
import com.example.ui.publicview.PublicEventQrScreen
import com.example.ui.superadmin.SuperAdminDashboardScreen
import com.example.ui.teacher.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EventSyncViewModel

class MainActivity : ComponentActivity() {
    private val deepLinkUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MyApplicationTheme {
                EventSyncApp(
                    deepLinkUrl = deepLinkUrlState.value,
                    onDeepLinkHandled = { deepLinkUrlState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val dataUri: Uri? = intent.data
        val dataString = intent.dataString
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        val targetUrl = dataString ?: dataUri?.toString() ?: sharedText
        if (!targetUrl.isNullOrBlank()) {
            deepLinkUrlState.value = targetUrl
        }
    }
}

@Composable
fun EventSyncApp(
    deepLinkUrl: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val viewModel: EventSyncViewModel = viewModel()
    val navController = rememberNavController()

    val currentUser by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    var currentScreen by remember { mutableStateOf("admin_events") }

    // Handle deep-link redirection when opening via link/intent
    LaunchedEffect(deepLinkUrl) {
        if (!deepLinkUrl.isNullOrBlank()) {
            val loadedEventId = viewModel.accessEventByCodeOrUrl(deepLinkUrl)
            if (loadedEventId != null) {
                viewModel.selectRole(UserRole.PUBLIC)
                viewModel.setSelectedEvent(loadedEventId)
                currentScreen = "public_overview"
                navController.navigate("public_overview") {
                    popUpTo(0) { inclusive = true }
                }
            }
            onDeepLinkHandled()
        }
    }

    // Synchronize default start screen when Role changes
    LaunchedEffect(currentRole) {
        currentScreen = when (currentRole) {
            UserRole.SUPER_ADMIN -> "super_dashboard"
            UserRole.ADMIN -> "admin_events"
            UserRole.TEACHER -> "teacher_dashboard"
            UserRole.PUBLIC -> "public_overview"
        }
        navController.navigate(currentScreen) {
            popUpTo(0) { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                currentUser = currentUser,
                currentRole = currentRole,
                onRoleSelected = { role ->
                    viewModel.selectRole(role)
                },
                onLogoutClicked = {
                    viewModel.logout()
                }
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentRole = currentRole,
                currentScreen = currentScreen,
                onNavigate = { route ->
                    currentScreen = route
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = currentScreen,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- SUPER ADMIN ROUTES ---
            composable("super_dashboard") {
                SuperAdminDashboardScreen(viewModel = viewModel)
            }
            composable("super_performance_types") {
                SuperAdminDashboardScreen(viewModel = viewModel)
            }
            composable("super_settings") {
                SuperAdminDashboardScreen(viewModel = viewModel)
            }

            // --- ADMIN ROUTES ---
            composable("admin_dashboard") {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToEventDetail = { eventId ->
                        viewModel.setSelectedEvent(eventId)
                        currentScreen = "admin_event_detail"
                        navController.navigate("admin_event_detail")
                    },
                    onNavigateToCreateEvent = {
                        currentScreen = "admin_event_create"
                        navController.navigate("admin_event_create")
                    },
                    onNavigateToTeachers = {
                        currentScreen = "admin_teachers"
                        navController.navigate("admin_teachers")
                    },
                    onNavigateToStudents = {
                        currentScreen = "admin_students"
                        navController.navigate("admin_students")
                    },
                    onNavigateToEditEvent = { eventId ->
                        viewModel.setSelectedEvent(eventId)
                        navController.navigate("admin_event_edit/$eventId")
                    }
                )
            }

            composable("admin_events") {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToEventDetail = { eventId ->
                        viewModel.setSelectedEvent(eventId)
                        navController.navigate("admin_event_detail")
                    },
                    onNavigateToCreateEvent = { navController.navigate("admin_event_create") },
                    onNavigateToTeachers = { navController.navigate("admin_teachers") },
                    onNavigateToStudents = { navController.navigate("admin_students") },
                    onNavigateToEditEvent = { eventId ->
                        viewModel.setSelectedEvent(eventId)
                        navController.navigate("admin_event_edit/$eventId")
                    }
                )
            }

            composable("admin_event_create") {
                CreateEventWizardScreen(
                    viewModel = viewModel,
                    onEventCreated = {
                        navController.popBackStack()
                        currentScreen = "admin_dashboard"
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }

            composable("admin_event_edit/{eventId}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                CreateEventWizardScreen(
                    viewModel = viewModel,
                    editingEventId = eventId,
                    onEventCreated = {
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }

            composable("admin_event_detail") {
                EventDetailAdminScreen(
                    viewModel = viewModel,
                    onNavigateToAddPerformance = { eventId ->
                        navController.navigate("teacher_add_performance/$eventId/new")
                    },
                    onNavigateToEditPerformance = { eventId, perfId ->
                        navController.navigate("teacher_add_performance/$eventId/$perfId")
                    }
                )
            }

            composable("admin_teachers") {
                TeachersScreen(viewModel = viewModel)
            }

            composable("admin_students") {
                ClassesAndStudentsScreen(viewModel = viewModel)
            }

            composable("admin_chat") {
                com.example.ui.components.GlobalChatScreen(viewModel = viewModel, currentRole = UserRole.ADMIN)
            }

            // --- TEACHER ROUTES ---
            composable("teacher_dashboard") {
                TeacherDashboardScreen(
                    viewModel = viewModel,
                    onOpenEvent = { eventId ->
                        viewModel.setSelectedEvent(eventId)
                        currentScreen = "teacher_event_detail"
                        navController.navigate("teacher_event_detail")
                    }
                )
            }

            composable("teacher_event_detail") {
                TeacherEventDetailScreen(
                    viewModel = viewModel,
                    onNavigateToAddPerformance = { eventId ->
                        navController.navigate("teacher_add_performance/$eventId/new")
                    },
                    onNavigateToEditPerformance = { eventId, perfId ->
                        navController.navigate("teacher_add_performance/$eventId/$perfId")
                    }
                )
            }

            composable("teacher_performances") {
                TeacherEventDetailScreen(
                    viewModel = viewModel,
                    onNavigateToAddPerformance = { eventId ->
                        navController.navigate("teacher_add_performance/$eventId/new")
                    },
                    onNavigateToEditPerformance = { eventId, perfId ->
                        navController.navigate("teacher_add_performance/$eventId/$perfId")
                    }
                )
            }

            composable("teacher_chat") {
                com.example.ui.components.GlobalChatScreen(viewModel = viewModel, currentRole = UserRole.TEACHER)
            }

            composable(
                route = "teacher_add_performance/{eventId}/{perfId}",
                arguments = listOf(
                    navArgument("eventId") { type = NavType.StringType },
                    navArgument("perfId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                val perfId = backStackEntry.arguments?.getString("perfId")
                val realPerfId = if (perfId == "new") null else perfId

                AddPerformanceScreen(
                    viewModel = viewModel,
                    eventId = eventId,
                    existingPerformanceId = realPerfId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // --- PUBLIC ROUTES ---
            composable("public_overview") {
                PublicEventOverviewScreen(viewModel = viewModel)
            }

            composable("public_history") {
                PublicEventHistoryScreen(
                    viewModel = viewModel,
                    onEventSelected = { eventId ->
                        viewModel.setSelectedEvent(eventId)
                        currentScreen = "public_overview"
                        navController.navigate("public_overview") {
                            popUpTo("public_overview") { inclusive = false }
                        }
                    },
                    onNavigateToQR = {
                        currentScreen = "public_qr"
                        navController.navigate("public_qr")
                    }
                )
            }

            composable("public_qr") {
                PublicEventQrScreen(
                    viewModel = viewModel,
                    onEventLoaded = { eventId ->
                        viewModel.setSelectedEvent(eventId)
                        currentScreen = "public_overview"
                        navController.navigate("public_overview") {
                            popUpTo("public_overview") { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
