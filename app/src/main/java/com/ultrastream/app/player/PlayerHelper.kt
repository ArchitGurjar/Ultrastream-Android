package com.ultrastream.app.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.ultrastream.app.MainActivity
import com.ultrastream.app.R

@UnstableApi
object PlayerHelper {

    fun enterPictureInPicture(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }

    fun openInExternalPlayer(activity: Activity, url: String, title: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.putExtra(Intent.EXTRA_TITLE, title)
            activity.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            // Fallback to browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
        }
    }

    fun sharePlaylist(activity: Activity, fileUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/x-mpegurl"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, "Share Playlist"))
    }

    fun getNotificationIntent(activity: Activity): PendingIntent {
        val intent = Intent(activity, MainActivity::class.java)
        return PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
