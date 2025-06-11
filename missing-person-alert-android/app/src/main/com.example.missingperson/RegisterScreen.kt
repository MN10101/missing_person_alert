package com.example.missingperson

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Registrierung",
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
                    val result = register(context, username, password)
                    if (result.isSuccess) {
                        onRegisterSuccess()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Registrierung fehlgeschlagen"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank()
        ) {
            Text("Registrieren")
        }
    }
}

suspend fun register(context: Context, username: String, password: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("role", "POLICE")
                .build()
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/register")
                .post(formBody)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body?.string() ?: "Unbekannter Fehler"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}