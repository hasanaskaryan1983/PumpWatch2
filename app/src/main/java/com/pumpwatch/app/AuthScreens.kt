package com.pumpwatch.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun SetupLoginScreen(error: String?, onCreate: (String, String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column {
            Text("راه‌اندازی ورود اپ", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "چون این اپ فقط محلی و بدون سرور کار می‌کند، رمز عبور در جایی ذخیره یا " +
                    "پشتیبان‌گیری نمی‌شود. اگر فراموشش کنی، تنها راه، پاک کردن داده اپ و " +
                    "تنظیم دوباره است — پس جایی یادداشتش کن.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("نام کاربری") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("رمز عبور (حداقل ۶ کاراکتر)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                label = { Text("تکرار رمز عبور") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onCreate(username, password, confirm) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ساخت ورود") }
        }
    }
}

@Composable
fun LoginScreen(error: String?, onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column {
            Text("ورود به PumpWatch", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("نام کاربری") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("رمز عبور") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onLogin(username, password) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ورود") }
        }
    }
}
