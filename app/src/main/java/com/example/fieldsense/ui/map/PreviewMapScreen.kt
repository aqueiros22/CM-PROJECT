package com.example.fieldsense.ui.map

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.ui.theme.Shapes
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@Composable
fun PreviewMapScreen(onBack: () -> Unit, previewLocation: Position){
    Log.d("PreviewMapScreen", "Previewing map")
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = previewLocation,
            zoom = 10.0
        )
    )
    val coroutineScope = rememberCoroutineScope()
    val recenterCamera: () -> Unit = {
        coroutineScope.launch { cameraState.animateTo(
            CameraPosition(
                target = previewLocation,
                zoom = 10.0
            )
        ) }
    }

    Box(modifier = Modifier.fillMaxSize()){
        MaplibreMap(
            modifier = Modifier,
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}")
        )
        Row(modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp)
        ) {
            Button(onClick = onBack, shape = Shapes.medium, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Button(onClick = recenterCamera, shape = Shapes.medium) {
                Icon(Icons.Default.LocationOn, contentDescription = "Recenter")
            }
        }
    }

}