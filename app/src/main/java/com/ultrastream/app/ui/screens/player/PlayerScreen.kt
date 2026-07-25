@file:OptIn(ExperimentalMaterial3Api::class)

package com.ultrastream.app.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.player.PlayerHelper
import com.ultrastream.app.ui.theme.AccentBlue
import com.ultrastream.app.utils.findActivity
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    stream: StreamItem,
    title: String = "Now Playing",
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context.findActivity() // ✅ हमेशा सही Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current

    val player by viewModel.player.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val error by viewModel.error.collectAsState()
    val playerTitle by viewModel.title.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val seekMessage by viewModel.seekMessage.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val availableQualities by viewModel.availableQualities.collectAsState()
    val subtitleTracks by viewModel.subtitleTracks.collectAsState()
    val currentSpeed by viewModel.speed.collectAsState()

    var showQualitySheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    LaunchedEffect(stream) {
        viewModel.initializePlayer(context, stream, title)
    }

    DisposableEffect(lifecycleOwner) {
        val listener = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!(activity?.isInPictureInPictureMode ?: false)) {
                        viewModel.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!isLocked) viewModel.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(listener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(listener)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    LaunchedEffect(brightness) {
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness
            window.attributes = layoutParams
        }
    }

    LaunchedEffect(volume) {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = (volume * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
    }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowInsetsControllerCompat(activity?.window!!, view).hide(WindowInsetsCompat.Type.systemBars())
            WindowInsetsControllerCompat(activity?.window!!, view).systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowInsetsControllerCompat(activity?.window!!, view).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = resizeMode
            }
        )

        if (!isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val width = size.width
                                val seekTime = if (offset.x < width / 2) -10000L else 10000L
                                viewModel.seekBy(seekTime)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val width = size.width
                                val deltaX = dragAmount.x / width
                                if (change.position.x < width / 2) {
                                    val newBrightness = (brightness + deltaX * 2).coerceIn(0f, 1f)
                                    viewModel.setBrightness(newBrightness)
                                } else {
                                    val newVolume = (volume + deltaX * 2).coerceIn(0f, 1f)
                                    viewModel.setVolume(newVolume)
                                }
                            }
                        )
                    }
            )
        }

        if (seekMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = seekMessage!!,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (playerTitle.isNotEmpty()) playerTitle else title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { viewModel.toggleLock() }) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isLocked) "Unlock" else "Lock",
                            tint = Color.White
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = { activity?.enterPictureInPictureMode() }) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = "Picture in Picture", tint = Color.White)
                        }
                    }
                    // Download / Open in external player
                    IconButton(
                        onClick = {
                            val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                            if (!url.isNullOrBlank()) {
                                if (url.startsWith("magnet:")) {
                                    clipboard.setText(AnnotatedString(url))
                                    android.widget.Toast.makeText(context, "Magnet copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    PlayerHelper.openInExternalPlayer(activity ?: context, url, title)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Download/Open", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.toggleFullscreen() }) {
                        Icon(
                            if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = AccentBlue,
                trackColor = Color.Gray.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSpeedSheet = true }) {
                    Text(
                        text = "${currentSpeed}x",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                IconButton(onClick = { viewModel.skipBackward() }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Back 10s", tint = Color.White)
                }
                IconButton(onClick = { viewModel.playPause() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { viewModel.skipForward() }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                }
                IconButton(onClick = { showQualitySheet = true }) {
                    Icon(Icons.Default.Hd, contentDescription = "Quality", tint = Color.White)
                }
                IconButton(onClick = { showSubtitleSheet = true }) {
                    Icon(Icons.Default.ClosedCaption, contentDescription = "Subtitles", tint = Color.White)
                }
            }
        }

        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    if (showQualitySheet) {
        ModalBottomSheet(onDismissRequest = { showQualitySheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Quality", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(availableQualities) { quality ->
                        ListItem(
                            headlineContent = { Text(quality.label) },
                            supportingContent = { Text(quality.resolution ?: "") },
                            modifier = Modifier.clickable {
                                viewModel.selectQuality(quality)
                                showQualitySheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSubtitleSheet) {
        ModalBottomSheet(onDismissRequest = { showSubtitleSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Subtitles", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text("Off") },
                            modifier = Modifier.clickable {
                                viewModel.disableSubtitles()
                                showSubtitleSheet = false
                            }
                        )
                    }
                    items(subtitleTracks) { track ->
                        ListItem(
                            headlineContent = { Text(track.label) },
                            supportingContent = { Text(track.language) },
                            modifier = Modifier.clickable {
                                viewModel.selectSubtitleTrack(track)
                                showSubtitleSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSpeedSheet) {
        ModalBottomSheet(onDismissRequest = { showSpeedSheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Playback Speed", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
                LazyColumn {
                    items(speeds) { speed ->
                        ListItem(
                            headlineContent = { Text("${speed}x") },
                            modifier = Modifier.clickable {
                                viewModel.setSpeed(speed)
                                showSpeedSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val seconds = millis / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        "%d:%02d:%02d".format(hours, minutes % 60, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
