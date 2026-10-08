package com.carvision.ai

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarVisionApp() }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    @Composable
    private fun CarVisionApp() {
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var result by remember { mutableStateOf<CarResult?>(null) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        val picker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                imageUri = uri
                bitmap = runCatching { loadBitmap(uri) }.getOrNull()
                result = null
                error = null
            }
        }

        MaterialTheme {
            Scaffold(
                topBar = { TopAppBar(title = { Text("CarVision AI") }) }
            ) { padding ->
                Column(
                    Modifier.padding(padding).padding(20.dp)
                        .fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Car make • model • color",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Select a clear photo of a single car. The app first checks for a car on-device, then can use the configured AI vision service for make/model.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(18.dp))

                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected car",
                            modifier = Modifier.fillMaxWidth().height(260.dp)
                        )
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(260.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("No image selected") }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { picker.launch("image/*") }) {
                            Text("Choose Image")
                        }
                        OutlinedButton(
                            enabled = imageUri != null && !loading,
                            onClick = {
                                val b = bitmap ?: return@OutlinedButton
                                scope.launch {
                                    loading = true
                                    error = null
                                    try {
                                        result = CarAnalyzer.analyze(b)
                                    } catch (t: Throwable) {
                                        error = t.message ?: "Analysis failed"
                                    } finally { loading = false }
                                }
                            }
                        ) { Text("Analyze") }
                    }

                    Spacer(Modifier.height(20.dp))
                    if (loading) CircularProgressIndicator()
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    result?.let { r ->
                        Spacer(Modifier.height(12.dp))
                        ResultCard(r)
                    }
                }
            }
        }
    }

    @Composable
    private fun ResultCard(r: CarResult) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Detection Result", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                ResultRow("Vehicle", if (r.isCar) "Car detected" else "No confident car detected")
                ResultRow("Make", r.make)
                ResultRow("Model", r.model)
                ResultRow("Color", r.color)
                ResultRow("Confidence", r.confidence)
                if (r.notes.isNotBlank()) ResultRow("Notes", r.notes)
            }
        }
    }

    @Composable
    private fun ResultRow(label: String, value: String) {
        Column(Modifier.padding(vertical = 5.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

data class CarResult(
    val isCar: Boolean,
    val make: String,
    val model: String,
    val color: String,
    val confidence: String,
    val notes: String
)
