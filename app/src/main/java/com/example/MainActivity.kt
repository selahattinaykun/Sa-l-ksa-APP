package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        ).build()
        
        val repository = AppRepository(db.medicationDao(), db.medicationLogDao())
        val viewModelFactory = MediViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MediViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MediViewModel) {
    val navController = rememberNavController()
    
    val items = listOf(
        Triple("home", "İlaçlarım", Icons.Default.Home),
        Triple("history", "Geçmiş", Icons.Default.History),
        Triple("chat", "Geminal", Icons.Default.Person),
        Triple("emergency", "Acil", Icons.Default.Warning)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isMainScreen = items.any { it.first == currentDestination?.route }
            
            if (isMainScreen) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.third, contentDescription = screen.second) },
                            label = { Text(screen.second) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.first } == true,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onAddClick = { navController.navigate("addMed") },
                    onEditClick = { id -> navController.navigate("editMed/$id") }
                )
            }
            composable("history") {
                HistoryScreen(viewModel)
            }
            composable("chat") {
                com.example.ui.screens.ChatScreen()
            }
            composable("emergency") {
                EmergencyScreen()
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
