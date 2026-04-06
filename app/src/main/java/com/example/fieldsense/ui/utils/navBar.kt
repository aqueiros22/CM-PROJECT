package com.example.fieldsense.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fieldsense.Destination
import com.example.fieldsense.MainScreen
import com.example.fieldsense.ui.areas.AreaDrawingScreen
import com.example.fieldsense.ui.areas.AreaViewModelFactory
import com.example.fieldsense.ui.attachments.AttachmentViewModelFactory
import com.example.fieldsense.ui.checklist.ChecklistViewModelFactory
import com.example.fieldsense.ui.map.DownloadedMapsScreen
import com.example.fieldsense.ui.map.LocationViewModel
import com.example.fieldsense.ui.map.MapScreen
import com.example.fieldsense.ui.map.OfflineMapScreen
import com.example.fieldsense.ui.notes.NoteViewModelFactory
import com.example.fieldsense.ui.templates.TemplateViewModelFactory
import com.example.fieldsense.ui.templates.TemplatesScreen
import com.example.fieldsense.ui.visits.VisitViewModel
import org.maplibre.compose.offline.OfflineManager
import org.maplibre.compose.offline.rememberOfflineManager

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    userId: String,
    email: String,
    visitViewModel: VisitViewModel,
    locationViewModel: LocationViewModel,
    offlineManager: OfflineManager,
    onLogout: () -> Unit,
    noteFactory: NoteViewModelFactory,
    areaFactory: AreaViewModelFactory,
    attachmentFactory: AttachmentViewModelFactory,
    templateFactory: TemplateViewModelFactory,
    checklistFactory: ChecklistViewModelFactory
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
                        noteFactory = noteFactory,
                        attachmentFactory = attachmentFactory,
                        templateFactory = templateFactory,
                        checklistFactory = checklistFactory,
                        areaFactory = areaFactory,
                        onNavigateToDrawing = { visitId ->
                            navController.navigate("draw_area/$visitId")
                        }

                    )
                    Destination.MAP -> MapScreen(Modifier, locationViewModel, onNavigateToOfflineMap = {navController.navigate("offline_map")})
                    Destination.DOWNLOADED_MAPS -> DownloadedMapsScreen(offlineManager)
                    Destination.TEMPLATES -> TemplatesScreen(templateFactory)
                }
            }
        }
        composable("offline_map") { OfflineMapScreen({ navController.popBackStack() }, offlineManager, locationViewModel = locationViewModel) }
        composable("draw_area/{visitId}") { backStackEntry ->
            val visitIdStr = backStackEntry.arguments?.getString("visitId")
            val visitId = visitIdStr?.toIntOrNull()
            val visits by visitViewModel.visits.collectAsState()
            val archivedVisits by visitViewModel.archivedVisits.collectAsState()

            val visit = visits.find { it.id == visitId } ?: archivedVisits.find { it.id == visitId }

            if (visit != null) {
                AreaDrawingScreen(
                    visit = visit,
                    onMapAttach = {mapId ->
                        visitViewModel.updateVisit(visit.copy(map = mapId))
                    },
                    onBack = { navController.popBackStack() },
                    offlineManager = offlineManager,
                    viewModel = viewModel(factory = areaFactory)
                )
/*                VisitAreaDrawingScreen(
                    visit = visit,
                    onSave = { areaStr ->
                        visitViewModel.updateVisit(visit.copy(area = areaStr))
                        navController.popBackStack()
                    },
                    onMapAttach = {mapId ->
                        visitViewModel.updateVisit(visit.copy(map = mapId))
                    },
                    onBack = { navController.popBackStack() },
                    offlineManager = offlineManager
                )*/
            }
        }
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
    attachmentFactory: AttachmentViewModelFactory,
    templateFactory: TemplateViewModelFactory,
    checklistFactory: ChecklistViewModelFactory,
    areaFactory: AreaViewModelFactory
) {
    val offlineManager = rememberOfflineManager()
    val navController = rememberNavController()
    val startDestination = Destination.MAIN
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    val lightgreen = Color(0xF0E8F5E9)
    val green = MaterialTheme.colorScheme.primary
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets,
                containerColor = lightgreen,
                contentColor = green,
            ) {
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
                                contentDescription = destination.contentDescription,
                            )
                        } ,
                        colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFFC8E6C9)
                    )
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
                offlineManager,
                onLogout,
                noteFactory,
                areaFactory,
                attachmentFactory,
                templateFactory,
                checklistFactory
            )
        }

    }
}