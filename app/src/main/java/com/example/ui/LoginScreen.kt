package com.example.ui

import androidx.compose.runtime.Composable

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit = {}
) {
    com.example.auth.LoginScreen(onAuthSuccess = onAuthSuccess)
}
