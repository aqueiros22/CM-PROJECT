package com.example.fieldsense.ui.map

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.MainActivity
import com.example.fieldsense.location.LocationHelper
import com.example.fieldsense.ui.theme.Shapes
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlinx.coroutines.launch
import kotlin.collections.get

@Composable
fun MapScreen(modifier: Modifier = Modifier, viewModel: LocationViewModel, onNavigateToOfflineMap: () -> Unit) {

    val context = LocalContext.current
    val myLocationHelper = LocationHelper(context)
    DisplayLocation(modifier = modifier, myLocationHelper = myLocationHelper, viewModel, context = context, onNavigateToOfflineMap = onNavigateToOfflineMap)
}



@Composable
fun DisplayLocation(
    modifier: Modifier,
    myLocationHelper: LocationHelper,
    viewModel: LocationViewModel,
    context: Context,
    onNavigateToOfflineMap: () -> Unit
) {
    val location = viewModel.location.value
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(-8.6291, 41.1579),
            zoom = 5.0
        )
    )
    val coroutineScope = rememberCoroutineScope()

    val textFieldState: TextFieldState = rememberTextFieldState()
    val onSearch: (String) -> Unit =  { query -> viewModel.search(query) }
    val searchResults: List<String> = viewModel.searchResults.map { it.placeName }
    val onResultSelected = { placeName: String ->
        val feature = viewModel.searchResults.find { it.placeName == placeName }
        feature?.let {
            val longitude = feature.center[0]
            val latitude = feature.center[1]
            coroutineScope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(longitude, latitude),
                        zoom = 16.0
                    )
                )
            }
        } ?: run {
            Toast.makeText(context, "Localização não encontrada, tente novamente.", Toast.LENGTH_LONG).show()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if ( permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                myLocationHelper.requestLocationUpdates(viewModel= viewModel)
            } else {
                val rationaleRequired = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                if(rationaleRequired){
                    Toast.makeText(
                        context,
                        " Esta funcionalidade requer permissões de localização",
                        Toast.LENGTH_LONG).show()
                } else {
                    // need to set permission from settings
                    Toast.makeText(context,
                        "Ative as permissões de localização nas definições",
                        Toast.LENGTH_LONG).show()
                }


            }
        })


    Box(modifier = modifier.fillMaxSize()){

        MaplibreMap(
            modifier = Modifier,
            baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
            cameraState = cameraState
        ) {
            location?.let { latLng ->
                val locationSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        FeatureCollection(
                            Feature(
                                geometry = Point(Position(latLng.longitude, latLng.latitude)),
                                properties = JsonObject(content = emptyMap())
                            )
                        )
                    )
                )
                // 2. Outer pulsing ring
                CircleLayer(
                    id = "user-location-ring",
                    source = locationSource,
                    radius = const(18.dp),
                    color = const(Color(0x554285F4)),  // semi-transparent blue
                    strokeWidth = const(0.dp)
                )
            }
        }

    }

    SimpleSearchBar(textFieldState, onSearch, searchResults, viewModel = viewModel, onResultSelected = onResultSelected)
    Column( modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
        ) {
        if (location != null) {
            Text("Location: lat: ${location.latitude}, long: ${location.longitude}")
        } else {
            Text("Location not available")
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Button(shape = Shapes.medium, onClick = {
                if (myLocationHelper.hasLocationPermission(context)){
                    // permission granter -> update location
                    myLocationHelper.requestLocationUpdates(viewModel= viewModel)
                    location?.let {
                        coroutineScope.launch {
                            cameraState.animateTo(
                                CameraPosition(
                                    target = Position(it.longitude, it.latitude),
                                    zoom = 15.0
                                )
                            )
                        }
                    }
                }
                else {
                    // Request location permission
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

            }) {
                Text("Minha localização")
            }
            Button( shape = Shapes.medium, onClick = onNavigateToOfflineMap) {
                Text(text = "Mapas offline")
            }
        }





    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    onResultSelected: (String) -> Any,
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
) {
    // Controls expansion state of the search bar
    var expanded by rememberSaveable{ mutableStateOf(false)}

    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                        viewModel.clearResults()
                                    },
                    onSearch = {
                        viewModel.clearResults()
                        onSearch(textFieldState.text.toString())
                        Log.d("SearchBar", "Search results: $searchResults")
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Search") }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            // Display search results in a scrollable column
            Column(Modifier.verticalScroll(rememberScrollState())) {
                searchResults.forEach { result ->
                    ListItem(
                        headlineContent = { Text(result) },
                        modifier = Modifier
                            .clickable {
                                textFieldState.edit { replace(0, length, result) }
                                expanded = false

                                onResultSelected(result)
                            }
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}