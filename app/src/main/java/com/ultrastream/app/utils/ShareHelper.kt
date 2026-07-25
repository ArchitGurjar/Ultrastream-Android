package com.ultrastream.app.utils

import android.content.Context
import android.content.Intent

object ShareHelper {
    fun shareText(context: Context, text: String, subject: String = "UltraStream") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
