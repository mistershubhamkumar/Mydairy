package com.example.security

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

fun isBiometricSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

fun authenticateWithBiometrics(
    context: Context,
    title: String = "Unlock My Diary",
    subtitle: String = "Verify your identity to open your secret feelings",
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        onError("Biometric unlock requires Android 9 or higher. Please use your PIN or Password.")
        return
    }
    try {
        val executor = ContextCompat.getMainExecutor(context)
        val cancellationSignal = CancellationSignal()
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton("Use PIN/Password", executor) { _, _ ->
                onError("Cancelled")
            }
            .build()

        prompt.authenticate(
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString?.toString() ?: "Biometric authentication error")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric not recognized. Please try again.")
                }
            }
        )
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Biometric prompt unavailable on this device.")
    }
}
