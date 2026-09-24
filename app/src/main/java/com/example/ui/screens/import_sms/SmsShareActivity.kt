package com.example.ui.screens.import_sms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.MainActivity

object SharedSmsHolder {
    var sharedText: String? = null
}

class SmsShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        } else null

        if (!rawText.isNullOrBlank()) {
            var cleaned = rawText
            // If the shared text contains URL encoded space %20
            if (cleaned.contains("%20")) {
                cleaned = cleaned.replace("%20", " ")
            }
            // If the shared text uses '+' as space separator (e.g. url form-encoded without spaces)
            if (cleaned.contains("+") && !cleaned.contains(" ") && cleaned.contains(Regex("""[^\d\s]\+[^\d\s]"""))) {
                cleaned = cleaned.replace("+", " ")
            }

            Log.d("ShareDebug", "raw=$rawText, cleaned=$cleaned")

            SharedSmsHolder.sharedText = cleaned

            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shared_sms_text", cleaned)
                putExtra("navigate_to", "import_sms")
            }
            startActivity(mainIntent)
        }
        finish()
    }
}
