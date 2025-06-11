package com.example.missingperson

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.LifecycleCoroutineScope
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@Composable
fun MissingPersonApp(
    onPickImage: () -> Unit,
    selectedImageFile: File?,
    lifecycleScope: LifecycleCoroutineScope,
    userLocation: Pair<Double, Double>?,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var lastSeenLatitude by remember { mutableStateOf("") }
    var lastSeenLongitude by remember { mutableStateOf("") }
    var alerts by remember { mutableStateOf(listOf<Person>()) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        alerts = fetchAlerts(context, userLocation?.first, userLocation?.second)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.app_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.full_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = lastSeenLatitude,
                onValueChange = { lastSeenLatitude = it },
                label = { Text("Last Seen Latitude (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = lastSeenLongitude,
                onValueChange = { lastSeenLongitude = it },
                label = { Text("Last Seen Longitude (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onPickImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedImageFile == null) stringResource(id = R.string.select_image) else stringResource(id = R.string.image_selected))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("MissingPersonPrefs", Context.MODE_PRIVATE)
                    if (!prefs.getBoolean("isAuthenticated", false)) {
                        message = "Bitte zuerst anmelden"
                        return@Button
                    }
                    if (name.isNotBlank() && selectedImageFile != null) {
                        val lat = lastSeenLatitude.toDoubleOrNull()
                        if (lat != null && (lat < 47.3 || lat > 55.1)) {
                            message = "Fehler: Breitengrad muss zwischen 47.3 und 55.1 liegen"
                            return@Button
                        }
                        val lon = lastSeenLongitude.toDoubleOrNull()
                        if (lon != null && (lon < 5.9 || lon > 15.0)) {
                            message = "Fehler: Längengrad muss zwischen 5.9 und 15.0 liegen"
                            return@Button
                        }
                        lifecycleScope.launch {
                            message = publishAlert(
                                name,
                                selectedImageFile,
                                lat,
                                lon,
                                context
                            )
                            alerts = fetchAlerts(context, userLocation?.first, userLocation?.second)
                        }
                    } else {
                        message = "Bitte füllen Sie alle erforderlichen Felder aus"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && selectedImageFile != null
            ) {
                Text(stringResource(id = "publish_alert"))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Abmelden")
            }
            if (message.isNotEmpty()) {
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(alerts) { alert ->
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter("http://10.0.2.2:8080/api/persons/image/${alert.imagePath}"),
                                contentDescription = alert.fullName,
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(alert.fullName, fontWeight = FontWeight.Bold)
                                Text("Published: ${alert.publishedAt}")
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchAlerts(context: Context, userLatitude: Double?, userLongitude: Double?): List<Person> {
    val sharedPrefs = context.getSharedPreferences("MissingPersonPrefs", Context.MODE_PRIVATE)
    val cachedJson = sharedPrefs.getString("cachedAlerts", null)
    val gson = Gson()
    val type = object : TypeToken<List<Person>>() {}.type
    var cachedAlerts: List<Person> = if (cachedJson != null) {
        gson.fromJson(cachedJson, type) ?: listOf()
    } else {
        listOf()
    }

    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = if (userLatitude != null && userLongitude != null) {
                "http://10.0.2.2:8080/api/persons?page=0&size=10&userLatitude=$userLatitude&userLongitude=$userLongitude"
            } else {
                "http://10.0.2.2:8080/api/persons?page=0&size=10"
            }
            val requestBuilder = Request.Builder().url(url)
            val jsessionId = sharedPrefs.getString("JSESSIONID", null)
            if (jsessionId != null) {
                requestBuilder.addHeader("Cookie", "JSESSIONID=$jsessionId")
            }
            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                val alerts: List<Person> = gson.fromJson(json, type) ?: listOf()
                sharedPrefs.edit {
                    putString("cachedAlerts", json)
                    apply()
                }
                alerts
            } else {
                cachedAlerts
            }
        } catch (e: Exception) {
            cachedAlerts
        }
    }
}

suspend fun publishAlert(name: String, imageFile: File, lastSeenLatitude: Double?, lastSeenLongitude: Double?, context: Context): String {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart(
                    "image",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaType())
                )

            if (lastSeenLatitude != null) {
                requestBodyBuilder.addFormDataPart("lastSeenLatitude", lastSeenLatitude.toString())
            }
            if (lastSeenLongitude != null) {
                requestBodyBuilder.addFormDataPart("lastSeenLongitude", lastSeenLongitude.toString())
            }

            val requestBody = requestBodyBuilder.build()

            val sharedPrefs = context.getSharedPreferences("MissingPersonPrefs", Context.MODE_PRIVATE)
            val jsessionId = sharedPrefs.getString("JSESSIONID", null)
            val requestBuilder = Request.Builder()
                .url("http://10.0.2.2:8080/api/persons/publish")
                .post(requestBody)
            if (jsessionId != null) {
                requestBuilder.addHeader("Cookie", "JSESSIONID=$jsessionId")
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                "Meldung erfolgreich veröffentlicht!"
            } else {
                "Fehler: ${response.body?.string() ?: "Unbekannter Fehler"}"
            }
        } catch (e: Exception) {
            "Fehler: ${e.message ?: "Unbekannter Fehler"}"
        }
    }
}