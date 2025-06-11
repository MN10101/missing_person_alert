package com.example.missingperson

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.LifecycleCoroutineScope
import java.io.File

@Composable
fun AppNavGraph(
    lifecycleScope: LifecycleCoroutineScope,
    userLocation: Pair<Double, Double>?,
    onPickImage: () -> Unit,
    selectedImageFile: () -> File?
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("login") { popUpTo("register") { inclusive = true } } }
            )
        }
        composable("main") {
            MissingPersonApp(
                onPickImage = onPickImage,
                selectedImageFile = selectedImageFile(),
                lifecycleScope = lifecycleScope,
                userLocation = userLocation,
                onLogout = {
                    lifecycleScope.launch {
                        logout(navController.context)
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }
                }
            )
        }
    }
}

suspend fun logout(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("${BuildConfig.BASE_URL}/logout")
                .post(FormBody.Builder().build())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                context.getSharedPreferences("MissingPersonPrefs", Context.MODE_PRIVATE).edit {
                    remove("JSESSIONID")
                    putBoolean("isAuthenticated", false)
                    apply()
                }
            }
        } catch (e: Exception) {
            // Log error but proceed with local cleanup
            context.getSharedPreferences("MissingPersonPrefs", Context.MODE_PRIVATE).edit {
                remove("JSESSIONID")
                putBoolean("isAuthenticated", false)
                apply()
            }
        }
    }
}