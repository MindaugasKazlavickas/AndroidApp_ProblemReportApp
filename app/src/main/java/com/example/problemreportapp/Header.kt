package com.example.problemreportapp

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onHomeButtonClick: () -> Unit
) {
    val showLanguagePicker = remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        navigationIcon = {
            IconButton(
                onClick = { showLanguagePicker.value = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                val flagIcon = if (currentLanguage == "en") R.drawable.ic_english_flag else R.drawable.ic_lithuanian_flag
                Image(
                    painter = painterResource(id = flagIcon),
                    contentDescription = stringResource(id = R.string.left_button),
                    contentScale = ContentScale.Crop,
                )
            }

            if (showLanguagePicker.value) {
                LanguagePickerOverlay(
                    onDismiss = { showLanguagePicker.value = false },
                    onSelectLanguage = { selectedLanguage ->
                        onLanguageChange(selectedLanguage)
                        showLanguagePicker.value = false
                    }
                )
            }
        },
    )
}

@Composable
fun LanguagePickerOverlay(
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Language", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_english_flag),
                        contentDescription = "English flag",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ClickableText(
                        text = AnnotatedString("English"),
                        onClick = { onSelectLanguage("en") },
                        style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_lithuanian_flag),
                        contentDescription = "Lithuanian flag",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ClickableText(
                        text = AnnotatedString("Lithuanian"),
                        onClick = { onSelectLanguage("lt") },
                        style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Close", color = MaterialTheme.colorScheme.secondary)
            }
        },
        containerColor = MaterialTheme.colorScheme.secondary
    )
}

fun updateLocale(languageCode: String, context: Context) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val resources = context.resources
    val config = resources.configuration
    config.setLocale(locale)

    resources.updateConfiguration(config, resources.displayMetrics)

    if (context is Activity) {
        context.recreate()
    }
}

fun clearCollectedData() {
    // Clear keys, reset encryption data
}