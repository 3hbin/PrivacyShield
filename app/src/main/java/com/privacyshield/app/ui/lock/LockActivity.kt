package com.privacyshield.app.ui.lock

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.privacyshield.app.data.ShieldPreferences
import com.privacyshield.app.ui.components.PinPad
import com.privacyshield.app.ui.theme.PrivacyShieldTheme

class LockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val preferences = ShieldPreferences.get(this)
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(target, 0)).toString()
        }.getOrDefault(target)
        setContent {
            val settings = preferences.settings.value
            PrivacyShieldTheme(settings.themeMode, settings.seedColor) {
                var pin by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                val canBio = settings.biometricEnabled && canUseBiometric()
                BackHandler { moveTaskToBack(true) }
                LaunchedEffect(canBio) {
                    if (canBio) showBiometric(preferences, target)
                }
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Privacy Shield", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(label, style = MaterialTheme.typography.headlineMedium)
                    Text("Nhập mã PIN để mở", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                    PinPad(
                        value = pin,
                        maxLength = settings.pinLength,
                        onValueChange = { value ->
                            pin = value
                            error = null
                            if (value.length == settings.pinLength) {
                                if (preferences.verifyPin(value)) {
                                    preferences.markUnlocked(target)
                                    finish()
                                } else {
                                    error = "Mã PIN không đúng"
                                    pin = ""
                                }
                            }
                        },
                        onBiometric = if (canBio) {
                            { showBiometric(preferences, target) }
                        } else null
                    )
                }
            }
        }
    }

    private fun canUseBiometric(): Boolean {
        val manager = BiometricManager.from(this)
        val result = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometric(preferences: ShieldPreferences, target: String) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    preferences.markUnlocked(target)
                    finish()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Mở khóa ứng dụng")
            .setSubtitle("Dùng vân tay hoặc khuôn mặt")
            .setNegativeButtonText("Dùng mã PIN")
            .build()
        prompt.authenticate(info)
    }

    companion object {
        const val EXTRA_PACKAGE = "target_package"
    }
}
