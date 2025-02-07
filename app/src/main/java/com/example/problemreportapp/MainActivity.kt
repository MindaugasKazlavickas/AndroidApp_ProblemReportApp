package com.example.problemreportapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.problemreportapp.ui.theme.ProblemReportAppTheme
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProblemReportAppTheme {

                var currentLanguage by remember { mutableStateOf("en") }
                val context = LocalContext.current
                val navController = rememberNavController() // Move this up

                fun onLanguageChange(newLanguage: String) {
                    currentLanguage = newLanguage
                    updateLocale(newLanguage, context)
                }

                fun onHomeButtonClick() {
                    clearCollectedData()
                }

                key(currentLanguage) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HeaderBar(
                            currentLanguage = currentLanguage,
                            onLanguageChange = { selectedLanguage ->
                                currentLanguage = selectedLanguage
                            },
                            onHomeButtonClick = { /* Handle Home button click */ }
                        )

                        Navigation()
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: NavHostController, // Add navController as a parameter
) {
    var showCamera by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showCheckmark by remember { mutableStateOf(false) }
    var textMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    var locationReceived by remember { mutableStateOf(false) }
    var locationCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var messageReceived by remember { mutableStateOf(false) }
    var photoTaken by remember { mutableStateOf(false) }

    val progress = (listOf(showCheckmark, locationReceived, messageReceived).count { it } / 3f)

    val progressPercentage = when {
        progress == 1f -> 100
        progress >= 0.66f -> 66
        progress >= 0.33f -> 33
        else -> 0
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                showCamera = true
            }
        }
    )

    val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                requestLocationAccess(context) { coordinates ->
                    locationCoordinates = coordinates
                    locationReceived = true
                }
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (!showCamera) {
            if (!photoTaken && !showCheckmark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📷",
                            fontSize = 72.sp
                        )
                    }
                }
            } else {
                    Text(
                        text = stringResource(id = R.string.start_prompt),
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(16.dp)
                    )
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Button(
                        onClick = {
                            showCamera = true
                            photoTaken = false
                            showCheckmark = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(end = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(text = stringResource(id = R.string.photo_taken))
                    }

                    if (showCheckmark) {
                        Text(
                            text = stringResource(id = R.string.checkmark),
                            style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }

        if (showCheckmark) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                Button(
                    onClick = {
                        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    enabled = !locationReceived,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .padding(end = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (locationReceived) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        contentColor = if (locationReceived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    ),
                    border = if (locationReceived) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null // Default border
                    }
                ) {
                    Text(
                        text = if (locationReceived) stringResource(id = R.string.location_saved) else stringResource(id = R.string.add_location),
                        color = if (locationReceived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                }

                if (locationReceived) {
                    Text(
                        text = stringResource(id = R.string.checkmark),
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .wrapContentSize(Alignment.Center)
                    )
                }
            }
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                MessageInputSection(
                    textMessage = textMessage,
                    onTextMessageChange = { textMessage = it },
                    messageReceived = messageReceived,
                    onMessageSubmit = {
                        messageReceived = true
                    }
                )
            }
            val primaryColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 16.dp
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = primaryColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                Text(
                    text = "$progressPercentage%",
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary)
                )
            }

            if (progress == 1f) {
                Button(
                    onClick = {
                        val uriString = imageUri?.toString() ?: ""
                        val coordinatesString = locationCoordinates?.let { "${it.first},${it.second}" } ?: ""
                        val messageString = textMessage
                        Log.d("Navigation", "Navigating to SubmissionScreen with:")
                        Log.d("Navigation", "imageUri: ${imageUri.toString()}")
                        Log.d("Navigation", "coordinates: ${locationCoordinates?.first}, ${locationCoordinates?.second}")
                        Log.d("Navigation", "message: $textMessage")
                        navController.navigate("submission?imageUri=$uriString&coordinates=$coordinatesString&message=$messageString")
                    },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Submit Report", color = MaterialTheme.colorScheme.tertiary)
                }
            }

        }
        } else {
            val lifecycleOwner = LocalLifecycleOwner.current
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                onImageCaptured = { uri ->
                    imageUri = uri
                    showCamera = false
                    showCheckmark = true
                }
            )
        }
    }

}

public fun requestLocationAccess(context: Context, onLocationUpdated: (Pair<Double, Double>) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            location?.let {
                val coordinates = Pair(it.latitude, it.longitude)
                onLocationUpdated(coordinates)
            } ?: run {
                Log.e(ContentValues.TAG, "Failed to get location")
            }
        }
        .addOnFailureListener { e ->
            Log.e(ContentValues.TAG, "Error getting location: ${e.message}")
        }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ProblemReportAppTheme {
        Navigation()
    }
}