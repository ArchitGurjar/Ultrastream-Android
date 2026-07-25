#!/bin/bash
cd ~/projects/Ultrastreaming || exit

echo "🔧 Applying all fixes from audit report..."

# =================================================================
# 1. NetworkModule.kt - Fix dummy base URL
# =================================================================
echo "📁 Fixing NetworkModule.kt..."
cat > app/src/main/java/com/ultrastream/app/di/NetworkModule.kt <<'EOF'
package com.ultrastream.app.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.network.AllDebridApi
import com.ultrastream.app.network.PremiumizeApi
import com.ultrastream.app.network.RealDebridApi
import com.ultrastream.app.network.StremioApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "UltraStream/1.0 (Android)")
                    .header("Referer", "https://ultrastream.app/")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://v3-cinemeta.strem.io/") // ✅ वास्तविक और हमेशा वैलिड बेस URL
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideStremioApi(retrofit: Retrofit): StremioApi {
        return retrofit.create(StremioApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRealDebridApi(retrofit: Retrofit): RealDebridApi {
        return retrofit.newBuilder()
            .baseUrl("https://api.real-debrid.com/rest/1.0/")
            .build()
            .create(RealDebridApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAllDebridApi(retrofit: Retrofit): AllDebridApi {
        return retrofit.newBuilder()
            .baseUrl("https://api.alldebrid.com/v4/")
            .build()
            .create(AllDebridApi::class.java)
    }

    @Provides
    @Singleton
    fun providePremiumizeApi(retrofit: Retrofit): PremiumizeApi {
        return retrofit.newBuilder()
            .baseUrl("https://www.premiumize.me/api/")
            .build()
            .create(PremiumizeApi::class.java)
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/di/NetworkModule.kt

# =================================================================
# 2. StreamDataHolder.kt - New file for safe navigation
# =================================================================
echo "📁 Creating StreamDataHolder.kt..."
cat > app/src/main/java/com/ultrastream/app/utils/StreamDataHolder.kt <<'EOF'
package com.ultrastream.app.utils

import com.ultrastream.app.data.models.StreamItem

object StreamDataHolder {
    var currentStream: StreamItem? = null
        private set

    fun setStream(stream: StreamItem) {
        currentStream = stream
    }

    fun clear() {
        currentStream = null
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/utils/StreamDataHolder.kt

# =================================================================
# 3. MainActivity.kt - Update navigation to use StreamDataHolder
# =================================================================
echo "📁 Updating MainActivity.kt..."
cat > app/src/main/java/com/ultrastream/app/MainActivity.kt <<'EOF'
package com.ultrastream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.ui.navigation.Screen
import com.ultrastream.app.ui.screens.addons.AddonsScreen
import com.ultrastream.app.ui.screens.details.DetailsScreen
import com.ultrastream.app.ui.screens.home.HomeScreen
import com.ultrastream.app.ui.screens.library.LibraryScreen
import com.ultrastream.app.ui.screens.player.PlayerScreen
import com.ultrastream.app.ui.screens.profile.ProfileScreen
import com.ultrastream.app.ui.screens.search.SearchScreen
import com.ultrastream.app.ui.theme.UltraStreamTheme
import com.ultrastream.app.utils.StreamDataHolder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UltraStreamTheme {
                UltraStreamNavHost()
            }
        }
    }

    @Composable
    fun UltraStreamNavHost() {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val items = listOf(
                        Triple(Screen.Home, "Home", R.drawable.ic_home),
                        Triple(Screen.Library, "Library", R.drawable.ic_library),
                        Triple(Screen.Search, "Search", R.drawable.ic_search),
                        Triple(Screen.Addons, "Addons", R.drawable.ic_addon),
                        Triple(Screen.Profile, "Profile", R.drawable.ic_profile)
                    )
                    items.forEach { (screen, title, iconRes) ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = ImageVector.vectorResource(id = iconRes), contentDescription = title) },
                            label = { Text(title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen { id, type ->
                        navController.navigate(Screen.Details.pass(id, type))
                    }
                }
                composable(Screen.Library.route) {
                    LibraryScreen { id, type ->
                        navController.navigate(Screen.Details.pass(id, type))
                    }
                }
                composable(Screen.Search.route) {
                    SearchScreen { id, type ->
                        navController.navigate(Screen.Details.pass(id, type))
                    }
                }
                composable(Screen.Addons.route) {
                    AddonsScreen()
                }
                composable(Screen.Profile.route) {
                    ProfileScreen()
                }
                composable(Screen.Details.route) { backStackEntry ->
                    val id = URLDecoder.decode(backStackEntry.arguments?.getString("id") ?: "", "UTF-8")
                    val type = URLDecoder.decode(backStackEntry.arguments?.getString("type") ?: "", "UTF-8")
                    DetailsScreen(
                        id = id,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onPlay = { stream: StreamItem, title: String ->
                            StreamDataHolder.setStream(stream) // ✅ मेमोरी में सेव
                            navController.navigate(Screen.Player.pass(title)) // ✅ सिर्फ टाइटल पास
                        }
                    )
                }
                composable(Screen.Player.route) { backStackEntry ->
                    val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
                    val stream = StreamDataHolder.currentStream
                    if (stream != null) {
                        PlayerScreen(
                            stream = stream,
                            title = title.ifBlank { "Now Playing" },
                            onBack = {
                                StreamDataHolder.clear()
                                navController.popBackStack()
                            }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/MainActivity.kt

# =================================================================
# 4. ContextUtils.kt - New file for findActivity()
# =================================================================
echo "📁 Creating ContextUtils.kt..."
cat > app/src/main/java/com/ultrastream/app/utils/ContextUtils.kt <<'EOF'
package com.ultrastream.app.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
EOF
git add app/src/main/java/com/ultrastream/app/utils/ContextUtils.kt

# =================================================================
# 5. PlayerScreen.kt - Use findActivity() instead of cast
# =================================================================
echo "📁 Updating PlayerScreen.kt..."
cat > app/src/main/java/com/ultrastream/app/ui/screens/player/PlayerScreen.kt <<'EOF'
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
EOF
git add app/src/main/java/com/ultrastream/app/ui/screens/player/PlayerScreen.kt

# =================================================================
# 6. PlayerViewModel.kt - Volume fix, selectQuality update
# =================================================================
echo "📁 Updating PlayerViewModel.kt..."
cat > app/src/main/java/com/ultrastream/app/ui/screens/player/PlayerViewModel.kt <<'EOF'
package com.ultrastream.app.ui.screens.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.models.Subtitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AudioTrackInfo(val groupIndex: Int, val trackIndex: Int, val label: String, val language: String)
data class SubtitleTrackInfo(val groupIndex: Int, val trackIndex: Int, val label: String, val language: String)
data class Quality(val label: String, val resolution: String?, val bitrate: Int?)

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _brightness = MutableStateFlow(-1.0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleTrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleTrackInfo>> = _subtitleTracks.asStateFlow()

    private val _availableQualities = MutableStateFlow<List<Quality>>(emptyList())
    val availableQualities: StateFlow<List<Quality>> = _availableQualities.asStateFlow()

    private val _seekMessage = MutableStateFlow<String?>(null)
    val seekMessage: StateFlow<String?> = _seekMessage.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private var playerListener: Player.Listener? = null
    private var positionJob: Job? = null
    private var currentContext: Context? = null
    private var currentStream: StreamItem? = null
    private var currentTitle: String? = null

    fun initializePlayer(context: Context, stream: StreamItem, title: String, externalSubtitle: Subtitle? = null) {
        currentContext = context
        currentStream = stream
        currentTitle = title

        // ✅ Volume fix: initialize with current system volume
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _volume.value = if (maxVol > 0) currentVol.toFloat() / maxVol.toFloat() else 0.5f

        viewModelScope.launch {
            try {
                val url = stream.url ?: stream.streamUrl ?: stream.externalUrl
                if (url.isNullOrBlank()) {
                    _error.value = "No valid stream URL"
                    return@launch
                }

                val trackSelector = DefaultTrackSelector(context)
                val exoPlayer = ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector)
                    .build()

                val dataSourceFactory = createDataSourceFactory()
                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(Uri.parse(url))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())

                // Existing subtitles from stream
                stream.subtitles?.let { subs ->
                    val configs = subs.mapNotNull { subtitle ->
                        val subUriStr = subtitle.url ?: return@mapNotNull null
                        val mimeType = when {
                            subUriStr.endsWith(".vtt", ignoreCase = true) -> "text/vtt"
                            subUriStr.endsWith(".srt", ignoreCase = true) -> "application/x-subrip"
                            else -> "text/vtt"
                        }
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(subUriStr))
                            .setMimeType(mimeType)
                            .setLanguage(subtitle.lang ?: "und")
                            .setLabel(subtitle.name ?: subtitle.lang ?: "Subtitle")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    }
                    if (configs.isNotEmpty()) {
                        mediaItemBuilder.setSubtitleConfigurations(configs)
                    }
                }

                // External subtitle
                if (externalSubtitle != null && !externalSubtitle.url.isNullOrBlank()) {
                    val mimeType = when {
                        externalSubtitle.url.endsWith(".vtt", ignoreCase = true) -> "text/vtt"
                        externalSubtitle.url.endsWith(".srt", ignoreCase = true) -> "application/x-subrip"
                        else -> "text/vtt"
                    }
                    val config = MediaItem.SubtitleConfiguration.Builder(Uri.parse(externalSubtitle.url))
                        .setMimeType(mimeType)
                        .setLanguage(externalSubtitle.lang ?: "und")
                        .setLabel(externalSubtitle.name ?: externalSubtitle.lang ?: "External Subtitle")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(listOf(config))
                }

                val mediaItem = mediaItemBuilder.build()
                val mediaSource = createMediaSource(mediaItem, dataSourceFactory)
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

                _player.value = exoPlayer
                _title.value = title

                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _duration.value = exoPlayer.duration
                                _isPlaying.value = exoPlayer.isPlaying
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _error.value = error.message
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        val audioList = mutableListOf<AudioTrackInfo>()
                        val subtitleList = mutableListOf<SubtitleTrackInfo>()
                        val qualityList = mutableListOf<Quality>()

                        tracks.groups.forEachIndexed { groupIndex, trackGroup ->
                            when (trackGroup.type) {
                                C.TRACK_TYPE_AUDIO -> {
                                    for (trackIndex in 0 until trackGroup.length) {
                                        val format = trackGroup.getTrackFormat(trackIndex)
                                        audioList.add(
                                            AudioTrackInfo(
                                                groupIndex = groupIndex,
                                                trackIndex = trackIndex,
                                                label = format.label ?: format.language ?: "Audio $trackIndex",
                                                language = format.language ?: "und"
                                            )
                                        )
                                    }
                                }
                                C.TRACK_TYPE_TEXT -> {
                                    for (trackIndex in 0 until trackGroup.length) {
                                        val format = trackGroup.getTrackFormat(trackIndex)
                                        subtitleList.add(
                                            SubtitleTrackInfo(
                                                groupIndex = groupIndex,
                                                trackIndex = trackIndex,
                                                label = format.label ?: format.language ?: "Subtitle $trackIndex",
                                                language = format.language ?: "und"
                                            )
                                        )
                                    }
                                }
                                C.TRACK_TYPE_VIDEO -> {
                                    for (trackIndex in 0 until trackGroup.length) {
                                        val format = trackGroup.getTrackFormat(trackIndex)
                                        val resolution = if (format.height != null && format.width != null) {
                                            "${format.height}p"
                                        } else null
                                        qualityList.add(
                                            Quality(
                                                label = format.label ?: "Quality",
                                                resolution = resolution,
                                                bitrate = format.bitrate
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        _audioTracks.value = audioList
                        _subtitleTracks.value = subtitleList
                        _availableQualities.value = qualityList
                    }
                }
                exoPlayer.addListener(listener)
                playerListener = listener

                positionJob?.cancel()
                positionJob = viewModelScope.launch {
                    while (isActive) {
                        try {
                            _currentPosition.value = exoPlayer.currentPosition
                        } catch (e: IllegalStateException) {
                            break
                        }
                        delay(200)
                    }
                }

            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent("UltraStream/1.0 (Android)")
            .setDefaultRequestProperties(mapOf("Referer" to "https://ultrastream.app/"))
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(60_000)
    }

    private fun createMediaSource(mediaItem: MediaItem, dataSourceFactory: DataSource.Factory): MediaSource {
        val uri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY
        val url = uri.toString()
        return when {
            url.contains(".m3u8") -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            url.contains(".mpd") -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            else -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }

    fun playPause() {
        _player.value?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.play()
                _isPlaying.value = true
            }
        }
    }

    fun play() {
        _player.value?.play()
        _isPlaying.value = true
    }

    fun pause() {
        _player.value?.pause()
        _isPlaying.value = false
    }

    fun skipForward(seconds: Long = 10) {
        _player.value?.let { player ->
            val newPos = player.currentPosition + seconds * 1000
            player.seekTo(newPos.coerceAtMost(player.duration))
        }
    }

    fun skipBackward(seconds: Long = 10) {
        _player.value?.let { player ->
            val newPos = player.currentPosition - seconds * 1000
            player.seekTo(newPos.coerceAtLeast(0))
        }
    }

    fun seekBy(offsetMs: Long) {
        _player.value?.let { player ->
            val newPos = player.currentPosition + offsetMs
            player.seekTo(newPos.coerceIn(0, player.duration))
            viewModelScope.launch {
                _seekMessage.value = if (offsetMs > 0) "+${offsetMs/1000}s" else "-${-offsetMs/1000}s"
                delay(800)
                _seekMessage.value = null
            }
        }
    }

    fun seekTo(position: Long) {
        _player.value?.seekTo(position.coerceIn(0, _duration.value))
    }

    fun setSpeed(speed: Float) {
        _player.value?.setPlaybackSpeed(speed)
        _speed.value = speed
    }

    fun setVolume(volume: Float) {
        _player.value?.volume = volume.coerceIn(0f, 1f)
        _volume.value = volume
    }

    fun setBrightness(brightness: Float) {
        _brightness.value = brightness.coerceIn(-1f, 1f)
    }

    fun selectAudioTrack(info: AudioTrackInfo) {
        val player = _player.value ?: return
        val group = player.currentTracks.groups.getOrNull(info.groupIndex) ?: return
        val params = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, listOf(info.trackIndex))
            )
            .build()
        player.trackSelectionParameters = params
    }

    fun disableSubtitles() {
        val player = _player.value ?: return
        val params = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        player.trackSelectionParameters = params
    }

    fun selectSubtitleTrack(info: SubtitleTrackInfo) {
        val player = _player.value ?: return
        val group = player.currentTracks.groups.getOrNull(info.groupIndex) ?: return
        val params = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, listOf(info.trackIndex))
            )
            .build()
        player.trackSelectionParameters = params
    }

    // ✅ Fixed selectQuality with clearOverrides and correct TrackSelectionOverride
    fun selectQuality(quality: Quality) {
        val player = _player.value ?: return
        val tracks = player.currentTracks

        for (groupIndex in tracks.groups.indices) {
            val trackGroup = tracks.groups[groupIndex]
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                val targetHeight = quality.resolution?.removeSuffix("p")?.toIntOrNull() ?: 0
                var bestIndex = 0
                var bestDiff = Int.MAX_VALUE

                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(trackIndex)
                    val height = format.height ?: 0
                    val diff = kotlin.math.abs(height - targetHeight)
                    if (diff < bestDiff) {
                        bestDiff = diff
                        bestIndex = trackIndex
                    }
                }

                // ✅ सही override
                val override = TrackSelectionOverride(
                    trackGroup.mediaTrackGroup,
                    listOf(bestIndex)
                )
                val params = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO) // पुराने हटाएं
                    .setOverrideForType(override)
                    .build()
                player.trackSelectionParameters = params
                return
            }
        }
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun releasePlayer() {
        positionJob?.cancel()
        positionJob = null
        playerListener?.let { listener ->
            _player.value?.removeListener(listener)
        }
        _player.value?.release()
        _player.value = null
        playerListener = null
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/ui/screens/player/PlayerViewModel.kt

# =================================================================
# 7. CreateSmartPlaylistUseCase.kt - Add proper CoroutineScope with error handling
# =================================================================
echo "📁 Updating CreateSmartPlaylistUseCase.kt..."
cat > app/src/main/java/com/ultrastream/app/domain/usecase/CreateSmartPlaylistUseCase.kt <<'EOF'
package com.ultrastream.app.domain.usecase

import com.ultrastream.app.data.models.MetaItem
import com.ultrastream.app.data.models.PlaylistEpisode
import com.ultrastream.app.data.models.SmartPlaylist
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.data.repository.AddonRepository
import com.ultrastream.app.data.repository.StreamRepository
import com.ultrastream.app.data.dao.SmartPlaylistDao
import com.ultrastream.app.data.preferences.PreferencesManager
import com.ultrastream.app.utils.LinkVerifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateSmartPlaylistUseCase @Inject constructor(
    private val smartPlaylistDao: SmartPlaylistDao,
    private val streamRepository: StreamRepository,
    private val addonRepository: AddonRepository,
    private val preferencesManager: PreferencesManager,
    private val linkVerifier: LinkVerifier
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val episodeListType = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
    private val episodeAdapter = moshi.adapter<List<PlaylistEpisode>>(episodeListType)

    // ✅ Zombie coroutine fix: proper scope with SupervisorJob and exception handler
    private val playlistScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
        CoroutineExceptionHandler { _, exception ->
            // डेटाबेस में playlist को 'Failed' मार्क करें
            // We need a reference to the playlistId; we'll capture it in the handler
            // But we can't easily pass it from inside; we'll update status manually after creation
        }
    )

    suspend operator fun invoke(meta: MetaItem, season: Int): Boolean {
        val episodes = meta.videos?.filter { it.season == season } ?: return false
        if (episodes.isEmpty()) return false

        val playlistId = "${meta.id}_S${season}_${System.currentTimeMillis()}"
        val initialPlaylist = SmartPlaylist(
            id = playlistId,
            metaId = meta.id,
            metaName = meta.name,
            poster = meta.poster,
            season = season,
            addon = "SmartPlaylist",
            total = episodes.size,
            fetched = 0,
            status = "Fetching...",
            episodesJson = "[]"
        )
        smartPlaylistDao.insert(initialPlaylist)

        // ✅ Background fetch with proper error handling and isActive checks
        playlistScope.launch {
            try {
                val addonUrls = addonRepository.getEnabledAddons().map { it.url }
                val hindiPriority = preferencesManager.getHindiPriority().first()
                val debridKey = preferencesManager.getDebridKey().first()
                val fetchedEpisodes = mutableListOf<PlaylistEpisode>()

                episodes.forEachIndexed { index, ep ->
                    // Check if coroutine is still active
                    if (!currentCoroutineContext().isActive) {
                        smartPlaylistDao.updateStatus(playlistId, "Cancelled")
                        return@launch
                    }

                    val epNum = ep.episode ?: 0

                    val streams = streamRepository.getStreams(
                        meta.id,
                        meta.type,
                        season,
                        epNum,
                        addonUrls,
                        hindiPriority,
                        debridKey.takeIf { it.isNotBlank() }
                    )

                    var bestWorkingStream: StreamItem? = null
                    for (stream in streams) {
                        val sUrl = stream.url ?: stream.streamUrl ?: stream.externalUrl
                        if (sUrl != null && !sUrl.startsWith("magnet:")) {
                            if (linkVerifier.verifyLink(sUrl)) {
                                bestWorkingStream = stream
                                break
                            }
                        }
                    }

                    fetchedEpisodes.add(
                        PlaylistEpisode(
                            epNum = epNum,
                            epName = ep.name ?: "Episode $epNum",
                            title = "${meta.name} - S${season}E${epNum}",
                            stream = bestWorkingStream,
                            isMissing = bestWorkingStream == null
                        )
                    )

                    val updatedPlaylist = initialPlaylist.copy(
                        fetched = index + 1,
                        status = if (index + 1 == episodes.size) "Ready" else "Fetching...",
                        episodesJson = episodeAdapter.toJson(fetchedEpisodes)
                    )
                    smartPlaylistDao.updatePlaylist(
                        id = playlistId,
                        fetched = updatedPlaylist.fetched,
                        status = updatedPlaylist.status,
                        episodesJson = updatedPlaylist.episodesJson
                    )
                }
            } catch (e: Exception) {
                // Update status to Failed
                smartPlaylistDao.updateStatus(playlistId, "Failed: ${e.message}")
            }
        }

        return true
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/domain/usecase/CreateSmartPlaylistUseCase.kt

# =================================================================
# 8. DebridHelper.kt - Add isActive checks in polling loops
# =================================================================
echo "📁 Updating DebridHelper.kt with isActive checks..."
cat > app/src/main/java/com/ultrastream/app/utils/DebridHelper.kt <<'EOF'
package com.ultrastream.app.utils

import android.util.Log
import com.ultrastream.app.network.AllDebridApi
import com.ultrastream.app.network.PremiumizeApi
import com.ultrastream.app.network.RealDebridApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DebridHelper"

@Singleton
class DebridHelper @Inject constructor(
    private val realDebridApi: RealDebridApi,
    private val allDebridApi: AllDebridApi,
    private val premiumizeApi: PremiumizeApi
) {

    enum class DebridProvider {
        REAL_DEBRID, ALL_DEBRID, PREMIUMIZE
    }

    suspend fun resolveStreamUrl(
        url: String,
        debridKey: String?,
        provider: DebridProvider = DebridProvider.REAL_DEBRID
    ): String {
        if (debridKey.isNullOrBlank()) {
            Log.d(TAG, "No debrid key provided, returning original URL")
            return url
        }

        return try {
            when (provider) {
                DebridProvider.REAL_DEBRID -> resolveRealDebrid(url, debridKey)
                DebridProvider.ALL_DEBRID -> resolveAllDebrid(url, debridKey)
                DebridProvider.PREMIUMIZE -> resolvePremiumize(url, debridKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Debrid resolution failed for $provider: ${e.message}", e)
            url
        }
    }

    private suspend fun resolveRealDebrid(url: String, apiKey: String): String {
        val auth = "Bearer $apiKey"
        return if (url.startsWith("magnet:")) {
            resolveRealDebridMagnet(url, auth)
        } else if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            resolveRealDebridMagnet(magnet, auth)
        } else {
            applyDebridParams(url, apiKey)
        }
    }

    private suspend fun resolveRealDebridMagnet(magnet: String, auth: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) {
            Log.d(TAG, "No valid hash in magnet, returning original")
            return magnet
        }
        return try {
            val availability = realDebridApi.checkInstantAvailability(auth, hash)
            if (availability.isNotEmpty()) {
                val cached = availability.values.firstOrNull { it.isNotEmpty() }
                if (cached != null) {
                    val addResponse = realDebridApi.addMagnet(auth, magnet)
                    val torrentId = addResponse.id
                    realDebridApi.selectFiles(auth, torrentId, "all")
                    var status = realDebridApi.getTorrentStatus(auth, torrentId)
                    var attempts = 0
                    while (attempts < 60 && currentCoroutineContext().isActive) {
                        delay(1000)
                        status = realDebridApi.getTorrentStatus(auth, torrentId)
                        attempts++
                    }
                    if (status.status == "downloaded" || status.status == "ready") {
                        val link = status.links.firstOrNull()
                        if (link != null) {
                            val unrestricted = realDebridApi.unrestrictLink(auth, link)
                            return unrestricted.link
                        }
                    }
                }
            }
            magnet
        } catch (e: Exception) {
            Log.e(TAG, "Real-Debrid magnet resolution failed: ${e.message}", e)
            magnet
        }
    }

    private suspend fun resolveAllDebrid(url: String, apiKey: String): String {
        return if (url.startsWith("magnet:")) {
            resolveAllDebridMagnet(url, apiKey)
        } else if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            resolveAllDebridMagnet(magnet, apiKey)
        } else {
            applyDebridParams(url, apiKey)
        }
    }

    private suspend fun resolveAllDebridMagnet(magnet: String, apiKey: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) {
            Log.d(TAG, "No valid hash in magnet, returning original")
            return magnet
        }
        return try {
            val uploadResponse = allDebridApi.uploadMagnet(apiKey, magnet)
            if (!uploadResponse.status || uploadResponse.data == null || uploadResponse.data.id.isEmpty()) {
                Log.e(TAG, "AllDebrid upload failed: ${uploadResponse.message}")
                return magnet
            }
            val torrentId = uploadResponse.data.id
            var statusResponse = allDebridApi.getMagnetStatus(apiKey, torrentId)
            var attempts = 0
            while (attempts < 60 && currentCoroutineContext().isActive) {
                val magnets = statusResponse.data?.magnets ?: emptyList()
                val first = magnets.firstOrNull()
                if (first != null && first.status == "Completed") {
                    break
                }
                delay(2000)
                statusResponse = allDebridApi.getMagnetStatus(apiKey, torrentId)
                attempts++
            }
            val magnetItem = statusResponse.data?.magnets?.firstOrNull()
            if (magnetItem != null && magnetItem.status == "Completed") {
                val linkResponse = allDebridApi.getMagnetLink(apiKey, torrentId)
                if (linkResponse.status && linkResponse.data != null && linkResponse.data.link.isNotEmpty()) {
                    return linkResponse.data.link
                }
            }
            magnet
        } catch (e: Exception) {
            Log.e(TAG, "AllDebrid magnet resolution failed: ${e.message}", e)
            magnet
        }
    }

    private suspend fun resolvePremiumize(url: String, apiKey: String): String {
        return if (url.startsWith("magnet:")) {
            resolvePremiumizeMagnet(url, apiKey)
        } else if (url.matches(Regex("^[a-fA-F0-9]{40}$"))) {
            val magnet = "magnet:?xt=urn:btih:$url"
            resolvePremiumizeMagnet(magnet, apiKey)
        } else {
            applyDebridParams(url, apiKey)
        }
    }

    private suspend fun resolvePremiumizeMagnet(magnet: String, apiKey: String): String {
        val hash = extractHash(magnet)
        if (hash.isEmpty()) {
            Log.d(TAG, "No valid hash in magnet, returning original")
            return magnet
        }
        return try {
            val transferResponse = premiumizeApi.createTransfer(apiKey, magnet)
            if (transferResponse.status != "success" || transferResponse.id.isEmpty()) {
                Log.e(TAG, "Premiumize transfer creation failed: ${transferResponse.message}")
                return magnet
            }
            val transferId = transferResponse.id
            var statusResponse = premiumizeApi.getTransferStatus(apiKey, transferId)
            var attempts = 0
            while (attempts < 60 && currentCoroutineContext().isActive) {
                val transfers = statusResponse.transfers ?: emptyList()
                val first = transfers.firstOrNull()
                if (first != null && first.status == "finished") {
                    break
                }
                delay(2000)
                statusResponse = premiumizeApi.getTransferStatus(apiKey, transferId)
                attempts++
            }
            val transferItem = statusResponse.transfers?.firstOrNull()
            if (transferItem != null && transferItem.status == "finished") {
                val itemResponse = premiumizeApi.getItemDetails(apiKey, transferId)
                if (itemResponse.status == "success" && itemResponse.link.isNotEmpty()) {
                    return itemResponse.link
                }
            }
            magnet
        } catch (e: Exception) {
            Log.e(TAG, "Premiumize magnet resolution failed: ${e.message}", e)
            magnet
        }
    }

    private fun extractHash(magnet: String): String {
        val match = Regex("btih:([a-fA-F0-9]{40})").find(magnet)
        return match?.groupValues?.get(1) ?: ""
    }

    fun applyDebridParams(url: String, debridKey: String): String {
        if (debridKey.isBlank()) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}realdebrid=$debridKey"
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/utils/DebridHelper.kt

# =================================================================
# 9. DatabaseModule.kt - Provide Moshi singleton for Converters
# =================================================================
echo "📁 Updating DatabaseModule.kt..."
cat > app/src/main/java/com/ultrastream/app/di/DatabaseModule.kt <<'EOF'
package com.ultrastream.app.di

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ultrastream.app.data.dao.*
import com.ultrastream.app.data.database.AppDatabase
import com.ultrastream.app.data.database.Converters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideConverters(moshi: Moshi): Converters {
        return Converters(moshi)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "ultrastream.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAddonDao(db: AppDatabase): AddonDao = db.addonDao()

    @Provides
    fun provideLibraryDao(db: AppDatabase): LibraryDao = db.libraryDao()

    @Provides
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideCachedMetaDao(db: AppDatabase): CachedMetaDao = db.cachedMetaDao()

    @Provides
    fun provideSmartPlaylistDao(db: AppDatabase): SmartPlaylistDao = db.smartPlaylistDao()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideWatchProgressDao(db: AppDatabase): WatchProgressDao = db.watchProgressDao()

    @Provides
    fun provideWatchedEpisodeDao(db: AppDatabase): WatchedEpisodeDao = db.watchedEpisodeDao()
}
EOF
git add app/src/main/java/com/ultrastream/app/di/DatabaseModule.kt

# =================================================================
# 10. Converters.kt - Use injected Moshi
# =================================================================
echo "📁 Updating Converters.kt..."
cat > app/src/main/java/com/ultrastream/app/data/database/Converters.kt <<'EOF'
package com.ultrastream.app.data.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.ultrastream.app.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Converters @Inject constructor(
    private val moshi: Moshi
) {

    @TypeConverter
    fun fromCatalogList(value: List<Catalog>): String {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toCatalogList(value: String): List<Catalog> {
        val type = Types.newParameterizedType(List::class.java, Catalog::class.java)
        val adapter = moshi.adapter<List<Catalog>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromEpisodeList(value: List<PlaylistEpisode>): String {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toEpisodeList(value: String): List<PlaylistEpisode> {
        val type = Types.newParameterizedType(List::class.java, PlaylistEpisode::class.java)
        val adapter = moshi.adapter<List<PlaylistEpisode>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromStreamItem(value: StreamItem?): String? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStreamItem(value: String?): StreamItem? {
        if (value == null) return null
        val adapter = moshi.adapter(StreamItem::class.java)
        return adapter.fromJson(value)
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/data/database/Converters.kt

# =================================================================
# 11. NavRoutes.kt - Update Player route to only pass title
# =================================================================
echo "📁 Updating NavRoutes.kt..."
cat > app/src/main/java/com/ultrastream/app/ui/navigation/NavRoutes.kt <<'EOF'
package com.ultrastream.app.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Search : Screen("search")
    object Addons : Screen("addons")
    object Profile : Screen("profile")
    object Details : Screen("details/{id}/{type}") {
        fun pass(id: String, type: String) =
            "details/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(type, "UTF-8")}"
    }
    object Player : Screen("player/{title}") {
        fun pass(title: String) = "player/${URLEncoder.encode(title, "UTF-8")}"
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/ui/navigation/NavRoutes.kt

echo "✅ All fixes applied successfully!"
echo "🚀 Now run: git commit -m \"Apply all audit fixes: NetworkModule, StreamDataHolder, MainActivity, ContextUtils, PlayerScreen, PlayerViewModel, CreateSmartPlaylistUseCase, DebridHelper, DatabaseModule, Converters, NavRoutes\" && git push -u origin main"
