package com.example.problemreportapp

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.ui.draw.alpha



@Composable
fun SubmissionScreen(
    navController: NavHostController,
    imageUri: Uri?,
    coordinates: Pair<Double, Double>?,
    message: String
) {
    val context = LocalContext.current
    var publicKeyString by remember { mutableStateOf<String?>(null) }
    var encryptedData by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val messageIncluded = if (message.isNotEmpty()) "1" else "0"
    val locationIncluded = if (coordinates != null) "1" else "0"
    val imageIncluded = "1" // Assuming the image is always included
    var secretKey by remember { mutableStateOf<SecretKey?>(null) }
/*
    LaunchedEffect(Unit) {
        fetchPublicKey(context) { key ->
            publicKeyString = key
        }
    }*/
    val identifiers = "$messageIncluded;$locationIncluded;$imageIncluded"
    Log.d("Identifiers", "Identifiers created: $identifiers")
    var currentMessageIndex by remember { mutableStateOf(0) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var loadingCompleted by remember { mutableStateOf(false) }
    val loadingMessages = listOf(
        "Encrypting data...",
        "Sending information...",
        "Processing image...",
        "Finalizing..."
    )

// Fade-out animation for the loading messages
    val fadeOutAlpha = animateFloatAsState(
        targetValue = if (loadingCompleted) 0f else 1f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        // Send the report in the background
        sendReport(message, context, imageUri, coordinates, "identifiers")
    }

    LaunchedEffect(currentMessageIndex) {
        if (currentMessageIndex < loadingMessages.size - 1) {
            delay(1000) // Wait 1 second before switching to the next message
            currentMessageIndex++
        } else {
            // Once the last message has been displayed, wait for 1 second and then fade out
            delay(1000)
            loadingCompleted = true
            delay(1000) // Allow time for fade-out animation
            showSuccessMessage = true // Show "Report Sent Successfully"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!loadingCompleted) {
            // Circular loading spinner
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 8.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display the loading message with a fade effect
            Text(
                text = loadingMessages[currentMessageIndex],
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.alpha(fadeOutAlpha.value)
            )
        }

        Log.d("Stuff", "$loadingCompleted")
        if(loadingCompleted) {
            Text(
                text = "Report Sent Successfully",
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(top = 16.dp)
            )
            Log.d("Stuff", "Displayed")
        }

    }
}

/*
CONTEXTAS:


Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        if (loading) {
            Text(text = "Loading...")
        } else {
            Text(text = "Image URI: $imageUri")
            Text(text = "Coordinates: ${coordinates?.first}, ${coordinates?.second}")
            Text(text = "Message: $message")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (imageUri != null && coordinates != null && message.isNotEmpty()) {
                    loading = true
                    try {
                        Log.d("Encryption", "Starting encryption process.")
                        secretKey = generateSymmetricKey()
                        encryptedData = encryptData(
                            "$message;${coordinates.first};${coordinates.second}".toByteArray(),
                            secretKey!!
                        ).first.let { Base64.encodeToString(it, Base64.DEFAULT) }

                        Log.d("Encryption", "Data encrypted: $encryptedData.")
                    } catch (e: Exception) {
                        Log.e("EncryptionError", "Failed to encrypt data: ${e.message}")
                    } finally {
                        loading = false
                    }
                } else {
                    Log.e("InputError", "One or more input values are null or empty.")
                }
            }) {
                Text("Encrypt")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display the encrypted message if it exists
            encryptedData?.let {
                Text(text = "Encrypted Data: $it", modifier = Modifier.padding(vertical = 8.dp))
            }

            Button(onClick = {
                // Send the encrypted message and image
                encryptedData?.let {
                    secretKey?.let { key ->
                        sendEncryptedMessage(encryptedData!!, context, key, Uri.parse(imageUri), identifiers, publicKeyString ?: return@let)
                    }
                }
            }) {
                Text(text = "Send Encrypted Message")
            }
        }
    }
 */


private fun generateSymmetricKey(): SecretKey {
    Log.d("GenerateSymmetricKey", "Generating AES symmetric key.")
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(256) // Use a 256-bit key for AES
    val key = keyGen.generateKey()
    Log.d("GenerateSymmetricKey", "Symmetric key generated successfully.")
    return key
}

private fun encryptData(data: ByteArray, secretKey: SecretKey): Pair<ByteArray, ByteArray> {
    Log.d("EncryptData", "Encrypting data using AES.")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) } // Generate random IV
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
    val encryptedData = cipher.doFinal(data)
    Log.d("EncryptData", "Data encrypted successfully. Encrypted size: ${encryptedData.size} bytes, IV: ${Base64.encodeToString(iv, Base64.DEFAULT)}.")
    return Pair(encryptedData, iv) // Return both encrypted data and IV
}

private fun encryptSymmetricKey(secretKey: SecretKey, publicKey: String): String? {
    // Convert the public key string into a PublicKey object
    val keySpec = X509EncodedKeySpec(Base64.decode(publicKey.replace("-----BEGIN PUBLIC KEY-----\n", "")
        .replace("-----END PUBLIC KEY-----\n", ""), Base64.DEFAULT))

    val keyFactory = KeyFactory.getInstance("RSA")
    val rsaPublicKey = keyFactory.generatePublic(keySpec)

    // Encrypt the symmetric key using RSA
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey)

    // Encrypt the symmetric key
    val encryptedKey = cipher.doFinal(secretKey.encoded) // This is correct, secretKey is of type SecretKey

    // Ensure proper padding and Base64 encoding
    val base64EncodedKey = Base64.encodeToString(encryptedKey, Base64.DEFAULT)
    Log.d("EncryptSymmetricKey", "Symmetric key encrypted and Base64-encoded: $base64EncodedKey")

    return base64EncodedKey
}


private fun compressImage(imageFile: File): ByteArray? {
    return try {
        Log.d("CompressImage", "Compressing image: ${imageFile.name}.")
        val bitmap = BitmapFactory.decodeFile(imageFile.path)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // Compress to JPEG with quality 80
        Log.d("CompressImage", "Image compressed successfully. Compressed size: ${outputStream.size()} bytes.")
        outputStream.toByteArray()
    } catch (e: Exception) {
        Log.e("CompressImage", "Failed to compress image: ${e.message}")
        null
    }
}

private fun encryptImage(imageBytes: ByteArray, secretKey: SecretKey): Pair<ByteArray, ByteArray>? {
    return try {
        Log.d("EncryptImage", "Encrypting image with AES.")
        val (encryptedImage, iv) = encryptData(imageBytes, secretKey)
        Log.d("EncryptImage", "Image encrypted successfully. Encrypted size: ${encryptedImage.size} bytes, IV: ${Base64.encodeToString(iv, Base64.DEFAULT)}.")
        Pair(encryptedImage, iv)
    } catch (e: Exception) {
        Log.e("EncryptImage", "Failed to encrypt image: ${e.message}")
        null
    }
}

// Create an OkHttpClient instance
private val client = OkHttpClient()


private fun fetchPublicKey(context: Context, onSuccess: (String) -> Unit) {
    val url = "http://10.0.2.2:5001/public-key"
    Log.d("FetchPublicKey", "Fetching public key from $url.")
    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            if (response.isSuccessful) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val publicKey = json.getString("public_key")
                        Log.d("FetchPublicKey", "Public key fetched successfully.")
                        onSuccess(publicKey)
                    } catch (e: Exception) {
                        Log.e("FetchPublicKey", "Failed to parse JSON: ${e.message}")
                    }
                } else {
                    Log.e("FetchPublicKey", "Response body is null")
                }
            } else {
                Log.e("FetchPublicKey", "Failed to fetch public key: ${response.message}")
            }
        }

        override fun onFailure(call: okhttp3.Call, e: IOException) {
            Log.e("FetchPublicKey", "Error: ${e.message}")
        }
    })
}

private fun sendEncryptedMessage(
    encryptedData: String,
    context: Context,
    secretKey: SecretKey,
    imageUri: Uri,
    identifiers: String,
    publicKeyString: String
) {
    val url = "http://10.0.2.2:5001/report"
    val imageFilePath = getFilePathFromUri(context, imageUri) ?: return
    val imageFile = File(imageFilePath)

    // Compress the image
    val compressedImageBytes = compressImage(imageFile)
    if (compressedImageBytes == null) {
        Log.e("SendEncryptedMessage", "Failed to compress image, aborting.")
        return
    }

    // Encrypt the compressed image
    val encryptedImageAndIv = encryptImage(compressedImageBytes, secretKey)
    if (encryptedImageAndIv == null) {
        Log.e("SendEncryptedMessage", "Failed to encrypt image, aborting.")
        return
    }
    val (encryptedImage, iv) = encryptedImageAndIv

    // Encrypt the symmetric key using the public key
    val encryptedSymmetricKey = encryptSymmetricKey(secretKey, publicKeyString)
    if (encryptedSymmetricKey == null) {
        Log.e("SendEncryptedMessage", "Failed to encrypt symmetric key, aborting.")
        return
    }

    // Prepare message and location data
    val messageToSend = if (encryptedData.isNotEmpty()) {
        encryptedData
    } else {
        // Default message if no data provided
        Base64.encodeToString("no message".toByteArray(), Base64.DEFAULT)
    }

    val locationToSend = if (identifiers[2] == '1') {
        // Example: Encrypt the location data if identifiers indicate it's included
        // Assuming you have the coordinates saved as a string, encrypt it here
        Base64.encodeToString("your_location_data".toByteArray(), Base64.DEFAULT) // Replace with actual data if available
    } else {
        // Default location if not provided
        Base64.encodeToString("no location".toByteArray(), Base64.DEFAULT)
    }

    // Prepare the request body
    Log.d("SendEncryptedMessage", "Preparing the request body.")
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("symmetric_key", encryptedSymmetricKey)
        .addFormDataPart("identifiers", identifiers)
        .addFormDataPart("image", imageFile.name, RequestBody.create("image/png".toMediaTypeOrNull(), encryptedImage))
        .addFormDataPart("iv", Base64.encodeToString(iv, Base64.DEFAULT))
        .addFormDataPart("message", messageToSend)  // Always add the encrypted message
        .addFormDataPart("location", locationToSend)  // Always add the encrypted location
        .build()

    Log.d("SendEncryptedMessage", "Sending data to server: symmetric_key=$encryptedSymmetricKey, identifiers=$identifiers, iv=${Base64.encodeToString(iv, Base64.DEFAULT)}, image size=${encryptedImage.size} bytes")

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                Log.d("SendEncryptedMessage", "Report sent successfully.")
            } else {
                Log.e("SendEncryptedMessage", "Failed to send report: ${response.message}")
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.e("SendEncryptedMessage", "Error: ${e.message}")
        }
    })
}

fun getFilePathFromUri(context: Context, uri: Uri): String? {
    // Check if the URI scheme is "content"
    if ("content" == uri.scheme) {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    return it.getString(columnIndex)
                }
            }
            null
        } finally {
            cursor?.close()
        }
    } else if ("file" == uri.scheme) {
        // If the scheme is "file", simply return the path
        return uri.path
    }
    return null
}

private fun sendReport(
    message: String,
    context: Context,
    imageUri: Uri?, // Change this from String? to Uri?
    coordinates: Pair<Double, Double>?,
    identifiers: String
) {
    val url = "http://10.0.2.2:5001/report"

    // Get the file path from the Uri
    val imageFilePath = imageUri?.let { getFilePathFromUri(context, it) }
    val imageFile = imageFilePath?.let { File(it) }

    // Prepare the request body
    Log.d("SendReport", "Preparing the request body.")
    val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

    // Add message and location
    requestBodyBuilder.addFormDataPart("message", message)

    coordinates?.let {
        val locationToSend = "${it.first},${it.second}"
        requestBodyBuilder.addFormDataPart("location", locationToSend)
    }

    // Add image if available
    imageFile?.let {
        val compressedImageBytes = compressImage(imageFile)
        if (compressedImageBytes != null) {
            requestBodyBuilder.addFormDataPart(
                "image",
                imageFile.name,
                RequestBody.create("image/png".toMediaTypeOrNull(), compressedImageBytes)
            )
        } else {
            Log.e("SendReport", "Failed to compress image, aborting.")
            return
        }
    }

    // Build the request body
    val requestBody = requestBodyBuilder.build()

    Log.d("SendReport", "Sending data to server: message=$message, location=$coordinates")

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                Log.d("SendReport", "Report sent successfully.")
            } else {
                Log.e("SendReport", "Failed to send report: ${response.message}")
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.e("SendReport", "Error: ${e.message}")
        }
    })
}
