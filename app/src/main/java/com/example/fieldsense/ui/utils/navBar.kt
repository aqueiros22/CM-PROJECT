package com.example.fieldsense.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fieldsense.Destination
import com.example.fieldsense.MainScreen
import com.example.fieldsense.ui.attachments.AttachmentViewModelFactory
import com.example.fieldsense.ui.map.LocationViewModel
import com.example.fieldsense.ui.map.MapScreen
import com.example.fieldsense.ui.map.OfflineMapScreen
import com.example.fieldsense.ui.notes.NoteViewModelFactory
import com.example.fieldsense.ui.visits.VisitViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    userId: String,
    email: String,
    visitViewModel: VisitViewModel,
    locationViewModel: LocationViewModel,
    onLogout: () -> Unit,
    noteFactory: NoteViewModelFactory,
    attachmentFactory: AttachmentViewModelFactory
) {
    NavHost(
        navController,
        startDestination = startDestination.route
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.MAIN -> MainScreen(
                        userId = userId,
                        email = email,
                        visitViewModel = visitViewModel,
                        onLogout = onLogout,
                        noteFactory =  noteFactory,
                        attachmentFactory = attachmentFactory)
                    Destination.MAP -> MapScreen(Modifier, locationViewModel, onNavigateToOfflineMap = {navController.navigate("offline_map")})
                }
            }
        }
        composable("offline_map") { OfflineMapScreen() }
    }
}

@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    userId: String,
    email: String,
    visitViewModel: VisitViewModel,
    locationViewModel: LocationViewModel,
    onLogout: () -> Unit,
    noteFactory: NoteViewModelFactory,
    attachmentFactory: AttachmentViewModelFactory
) {
    val navController = rememberNavController()
    val startDestination = Destination.MAIN
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.contentDescription
                            )
                        }

                    )
                }
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier.padding(contentPadding)
        ) {
            AppNavHost(
                navController,
                startDestination,
                userId,
                email,
                visitViewModel,
                locationViewModel,
                onLogout,
                noteFactory,
                attachmentFactory
            )
        }

    }
}