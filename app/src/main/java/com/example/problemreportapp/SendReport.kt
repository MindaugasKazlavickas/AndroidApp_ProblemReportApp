package com.example.problemreportapp

import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/*
        // Submit button
        Button(onClick = {
            navController.navigate("submission?imageUri=$imageUri&coordinates=${updatedCoordinates?.first},${updatedCoordinates?.second}&message=$textMessage")
        }) {
            Text(stringResource(id = R.string.submit))
        }
*/

@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var zoomState by remember { mutableFloatStateOf(0.5f) } // Initial zoom level set to 0.5
    var isFlashOn by remember { mutableStateOf(false) } // Flashlight state

    // Create ScaleGestureDetector for pinch-to-zoom
    val scaleGestureDetector = remember {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private val sensitivity = 0.1f // Sensitivity factor for zoom
            private var lastZoomState = zoomState // Store the last zoom state

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Calculate the new zoom level based on the pinch gesture
                val scaleChange = (detector.scaleFactor - 1) * sensitivity
                lastZoomState += scaleChange*2 // Incrementally adjust the zoom state
                zoomState = lastZoomState.coerceIn(0f, 1f) // Clamp to valid zoom range
                cameraControl?.setLinearZoom(zoomState) // Apply the zoom to the camera
                return true
            }
        })
    }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder().build() // Initialize ImageCapture
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                cameraControl = camera.cameraControl // Get camera control to adjust zoom

                // Set initial zoom to 0.5x immediately after binding
                cameraControl?.setLinearZoom(zoomState)
            } catch (ex: Exception) {
                Log.e(ContentValues.TAG, "Error starting camera preview: ${ex.message}", ex)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            previewView.bitmap?.recycle()
            previewView.clearFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            view.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                true
            }
        }

        // Flash Button
        Button(
            onClick = {
                isFlashOn = !isFlashOn
                imageCapture?.flashMode = if (isFlashOn) {
                    ImageCapture.FLASH_MODE_ON
                } else {
                    ImageCapture.FLASH_MODE_OFF
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFlashOn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                contentColor = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text(text = "Flash")
        }

        // Capture Button
        Button(
            onClick = {
                Log.d(ContentValues.TAG, "ImageCapture state before capture: $imageCapture")

                imageCapture?.let { capture ->
                    val outputDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
                    val photoFile = File(
                        outputDirectory,
                        "${SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis())}.jpg"
                    )

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    val startTime = System.currentTimeMillis()



                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val elapsedTime = System.currentTimeMillis() - startTime
                                Log.d(ContentValues.TAG, "Capture time: $elapsedTime ms")
                                val savedUri = Uri.fromFile(photoFile)
                                Toast.makeText(context, "Photo capture succeeded: $savedUri", Toast.LENGTH_SHORT).show()
                                onImageCaptured(savedUri)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e(ContentValues.TAG, "Photo capture failed: ${exc.message}", exc)
                            }
                        }
                    )
                } ?: Log.e(ContentValues.TAG, "ImageCapture is null.")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text(stringResource(id = R.string.capture),
                color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun MessageInputSection(
    textMessage: String,
    onTextMessageChange: (String) -> Unit,
    messageReceived: Boolean,
    onMessageSubmit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TextField(
            value = textMessage,
            onValueChange = onTextMessageChange,
            placeholder = {
                Text(text = stringResource(id = R.string.add_message),
                    color = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondary,
                unfocusedContainerColor = MaterialTheme.colorScheme.tertiary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (textMessage.isNotEmpty()) {
                        onMessageSubmit()
                        keyboardController?.hide()
                        focusRequester.freeFocus()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = if (!messageReceived) stringResource(id = R.string.submit_message) else stringResource(id = R.string.message_submitted),
                    color = MaterialTheme.colorScheme.tertiary)
            }

            if (messageReceived) {
                Text(
                    text = stringResource(id = R.string.checkmark),
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .wrapContentSize(Alignment.Center)
                )
            }
        }
    }
}



/*



 */



/*
@app.route('/report', methods=['POST'])
def report():
    print("Received a POST request.")
    print(f"Request headers: {request.headers}")

    # Log the incoming form data
    print(f"Form data received: {request.form}")
    print(f"Files received: {request.files}")

    try:
        # Retrieve the encrypted symmetric key, IV, and identifiers
        encrypted_key = request.form.get('symmetric_key')
        identifiers = request.form.get('identifiers')
        iv_base64 = request.form.get('iv')

        if not encrypted_key or not identifiers or not iv_base64:
            print("Missing encrypted key, identifiers, or IV.")
            return "Missing data", 400

        # Decode IV
        try:
            iv = base64.b64decode(iv_base64.strip())
            print(f"Received IV: {iv.hex()} (size: {len(iv)} bytes)")
        except Exception as e:
            print(f"Error decoding IV: {e}")
            return "Invalid IV format", 400

        # Load private key for decryption
        private_key = load_private_key()
        if private_key is None:
            print("Private key loading failed.")
            return "Server error: unable to load private key", 500

        # Decrypt the symmetric key
        symmetric_key = decrypt_symmetric_key(encrypted_key.strip(), private_key)
        if symmetric_key is None:
            print("Symmetric key decryption failed.")
            return "Decryption failed", 400

        print(f"Symmetric key size: {len(symmetric_key)} bytes")

        # Treat identifiers as plain text; no decoding necessary
        identifiers = identifiers.strip()
        print(f"Identifiers received: {identifiers}")

        # Split identifiers and check format
        identifiers_parts = identifiers.split(";")
        if len(identifiers_parts) != 3:
            print("Identifiers format error. Expected format: 'message;location;image'.")
            return "Invalid identifiers format", 400

        message_included, location_included, image_included = identifiers_parts

        # If the message and location are included, retrieve and decrypt them
        decrypted_message = None
        if message_included == '1':
            encrypted_message = request.form.get('message')
            if encrypted_message:
                try:
                    decrypted_message = decrypt_aes_data(base64.b64decode(encrypted_message.strip()), iv, symmetric_key)
                    print(f"Decrypted message: {decrypted_message}")
                except Exception as e:
                    print(f"Failed to decrypt message: {e}")

        decrypted_location = None
        if location_included == '1':
            encrypted_location = request.form.get('location')
            if encrypted_location:
                try:
                    decrypted_location = decrypt_aes_data(base64.b64decode(encrypted_location.strip()), iv, symmetric_key)
                    print(f"Decrypted location: {decrypted_location}")
                except Exception as e:
                    print(f"Failed to decrypt location: {e}")

        # Process and save the image file
        if image_included == '1' and 'image' in request.files:
            image = request.files['image']
            print(f"Received image: {image.filename}")

            folder_name = datetime.now().strftime("%Y%m%d_%H%M%S")
            folder_path = f"../received/{folder_name}"

            # Ensure the received directory exists
            os.makedirs(folder_path, exist_ok=True)

            image_path = os.path.join(folder_path, image.filename)
            image.save(image_path)
            print(f"Image saved at: {image_path}")
        else:
            print("No image provided.")

        # Save the decrypted message and location to a text file
        if decrypted_message or decrypted_location:
            text_file_path = os.path.join(folder_path, "report.txt")
            with open(text_file_path, 'w') as text_file:
                if decrypted_location:
                    text_file.write(f"Location: {decrypted_location}\n")
                if decrypted_message:
                    text_file.write(f"Message: {decrypted_message}\n")
            print(f"Report saved at: {text_file_path}")
        else:
            print("No decrypted message or location to save.")

        return "Problem received and logged", 200

    except Exception as e:
        print(f"Error processing the request: {e}")
        return "Server error", 500

 */