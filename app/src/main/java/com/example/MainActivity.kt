package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.screens.AddMedicationScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MediViewModel
import com.example.ui.components.UpdateDialog
import com.example.util.UpdateManager
import androidx.compose.ui.platform.LocalContext
import com.example.viewmodel.MediViewModelFactory

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "meditrack-db"
        )
        .fallbackToDestructiveMigration()
        .build()
        
        val repository = AppRepository(db.medicationDao(), db.medicationLogDao())
        val viewModelFactory = MediViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MediViewModel::class.java]

        setContent {
            val systemTheme = androidx.compose.foundation.isSystemInDarkTheme()
            val sharedPrefs = remember { getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) }
            var isDarkTheme by remember {
                mutableStateOf(sharedPrefs.getBoolean("is_dark", systemTheme))
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainApp(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = {
                        isDarkTheme = !isDarkTheme
                        sharedPrefs.edit().putBoolean("is_dark", isDarkTheme).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: MediViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UpdateManager.checkUpdate(context)
    }

    UpdateDialog()

    val navController = rememberNavController()
    
    val items = listOf(
        Triple("home", "İlaçlarım", Icons.Default.Home),
        Triple("history", "Geçmiş", Icons.Default.History),
        Triple("emergency", "SOS", Icons.Default.Warning),
        Triple("chat", "Asistan", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isMainScreen = items.any { it.first == currentDestination?.route }
            
            if (isMainScreen) {
                NavigationBar {
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.first } == true
                        if (screen.first == "emergency") {
                            val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "sos_scale"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 0.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "sos_alpha"
                            )

                            NavigationBarItem(
                                icon = {
                                    Box(contentAlignment = Alignment.Center) {
                                        // Pulse Ring behind
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .scale(pulseScale)
                                                .alpha(pulseAlpha)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.fillMaxSize()
                                            ) {}
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.size(48.dp),
                                            shadowElevation = 4.dp
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = "SOS",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = "SOS",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate("emergency") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            NavigationBarItem(
                                icon = { Icon(screen.third, contentDescription = screen.second) },
                                label = { Text(screen.second) },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.first) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onAddClick = { navController.navigate("addMed") },
                    onEditClick = { id -> navController.navigate("editMed/$id") }
                )
            }
            composable("history") {
                HistoryScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle
                )
            }
            composable("chat") {
                com.example.ui.screens.ChatScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle
                )
            }
            composable("emergency") {
                EmergencyScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle
                )
            }
            composable("addMed") {
                AddMedicationScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(
                route = "editMed/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id")
                AddMedicationScreen(
                    viewModel = viewModel,
                    medicationId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
