package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.FinTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FinTrackApp

        val initialSharedText = intent?.getStringExtra("shared_sms_text")
        val navigateToScanSms = intent?.getStringExtra("navigate_to") == "scan_sms"

        setContent {
            val themeModeState = remember {
                mutableStateOf(app.appModule.settingsRepository.themeMode)
            }
            val darkTheme = when (themeModeState.value) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            FinTrackTheme(darkTheme = darkTheme) {
                NavGraph(
                    app = app,
                    initialSharedSmsText = initialSharedText,
                    navigateToScanSms = navigateToScanSms,
                    onThemeChanged = { newMode ->
                        themeModeState.value = newMode
                    }
                )
            }
        }
    }
}
