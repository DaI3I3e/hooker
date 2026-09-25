package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.ui.navigation.NavGraph
import com.example.ui.screens.security.AppLockScreen
import com.example.ui.theme.FinTrackTheme

class MainActivity : FragmentActivity() {

    private var isAppLockedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FinTrackApp

        val initialSharedText = intent?.getStringExtra("shared_sms_text")
        val navigateToScanSms = intent?.getStringExtra("navigate_to") == "scan_sms"

        // Lock if biometric is enabled
        if (app.appModule.settingsRepository.isBiometricEnabled) {
            isAppLockedState.value = true
        }

        updateSecureFlags(app.appModule.settingsRepository.isSecureScreenEnabled)

        setContent {
            val themeModeState = remember {
                mutableStateOf(app.appModule.settingsRepository.themeMode)
            }
            val isLocked by remember { isAppLockedState }

            val darkTheme = when (themeModeState.value) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            // Sync secure flags whenever settings change
            LaunchedEffect(Unit) {
                updateSecureFlags(app.appModule.settingsRepository.isSecureScreenEnabled)
            }

            FinTrackTheme(darkTheme = darkTheme) {
                if (isLocked && app.appModule.settingsRepository.isBiometricEnabled) {
                    AppLockScreen(
                        onUnlock = {
                            isAppLockedState.value = false
                        }
                    )
                } else {
                    NavGraph(
                        app = app,
                        initialSharedSmsText = initialSharedText,
                        navigateToScanSms = navigateToScanSms,
                        onThemeChanged = { newMode ->
                            themeModeState.value = newMode
                            updateSecureFlags(app.appModule.settingsRepository.isSecureScreenEnabled)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as? FinTrackApp
        if (app != null && app.appModule.settingsRepository.isBiometricEnabled) {
            // Keep locked if already locked or lock on fresh resume if configured
        }
    }

    private fun updateSecureFlags(enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
