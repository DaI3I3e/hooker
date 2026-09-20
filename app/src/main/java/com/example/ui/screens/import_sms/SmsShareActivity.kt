package com.example.ui.screens.import_sms

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.MainActivity

class SmsShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        } else null

        val sharedText = rawText?.let { cleanSharedText(it) }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!sharedText.isNullOrBlank()) {
                putExtra("shared_sms_text", sharedText)
            }
        }
        startActivity(mainIntent)
        finish()
    }

    private fun cleanSharedText(text: String): String {
        return com.example.util.PatternExtractor.cleanSharedText(text)
    }
}
