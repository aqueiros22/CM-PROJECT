package com.example.fieldsense

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
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.coroutineScope
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
@Composable
fun MapScreen(modifier: Modifier = Modifier, viewModel: LocationViewModel) {
/*    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier,
            baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=PM5n8PfnTJ23rHV0J4bb")
        )
    }*/

    val context = LocalContext.current
    val myLocationHelper = LocationHelper(context)
    DisplayLocation(modifier = modifier, myLocationHelper = myLocationHelper, viewModel, context = context)
}



@Composable
fun DisplayLocation(
    modifier: Modifier,
    myLocationHelper: LocationHelper,
    viewModel: LocationViewModel,
    context: Context
) {
    val location = viewModel.location.value
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(38.7169, -9.1399),
            zoom = 5.0
        )
    )
    val coroutineScope = rememberCoroutineScope()
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
                        " This feature requires location permission",
                        Toast.LENGTH_LONG).show()
                } else {
                    // need to set permission from settings
                    Toast.makeText(context,
                        "Please, activate location permission in phone settings",
                        Toast.LENGTH_LONG).show()
                }


            }
        })


    Box(modifier = modifier.fillMaxSize()){

        MaplibreMap(
            modifier = Modifier,
            baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=PM5n8PfnTJ23rHV0J4bb"),
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

    Column( modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
        ) {
        if (location != null) {
            Text("Location: lat: ${location.latitude}, long: ${location.longitude}")
        } else {
            Text("Location not available")
        }

        Button(onClick = {
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
            Text("Get Location")
        }




    }
}