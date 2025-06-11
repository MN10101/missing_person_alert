package com.example.missingperson

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpCookie

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(context.getSharedPreferences("MissingPersonPrefs", Context.MODE_PRIVATE).getBoolean("isAuthenticated", false)) }

    if (isAuthenticated) {
        MissingPersonApp(
            onPickImage = { /* Existing logic from MainActivity */ },
            selectedImageFile = null,
            lifecycleScope = (context as MainActivity).lifecycleScope,
            userLocation = (context as MainActivity).userLocation
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.app_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Benutzername") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    isLoading = true
                    (context as MainActivity).lifecycleScope.launch {
                        val result = login(context, username, password)
                        if (result.isSuccess) {
                            isAuthenticated = true
                            onLoginSuccess()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Ungültiger Benutzername oder Passwort"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text("Anmelden")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { navController.navigate("register") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kein Konto? Hier registrieren")
            }
        }
    }
}

suspend fun login(context: Context, username: String, password: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build()
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/login")
                .post(formBody)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                // Extract JSESSIONID from Set-Cookie header
                val cookies = response.header("Set-Cookie")?.let { HttpCookie.parse(it) } ?: emptyList()
                val jsessionId = cookies.find { it.name == "JSESSIONID" }?.value
                if (jsessionId != null) {
                    context.getSharedPreferences("MissingPersonPrefs", Context.MODE_PRIVATE).edit {
                        putString("JSESSIONID", jsessionId)
                        putBoolean("isAuthenticated", true)
                        apply()
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Kein Authentifizierungs-Cookie erhalten"))
                }
            } else {
                Result.failure(Exception("Ungültiger Benutzername oder Passwort"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}