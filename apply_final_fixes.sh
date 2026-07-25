#!/bin/bash
cd ~/projects/Ultrastreaming || exit

echo "🔧 Applying final missing pieces..."

# =================================================================
# 1. app/build.gradle.kts - Add media3-session dependency
# =================================================================
echo "📁 Updating app/build.gradle.kts with media3-session..."
cat > app/build.gradle.kts <<'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.ultrastream.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ultrastream.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.0")
    implementation("androidx.media3:media3-session:1.2.0") // ✅ नई डिपेंडेंसी

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
EOF
git add app/build.gradle.kts

# =================================================================
# 2. MainActivity.kt - Use Material Icons instead of drawable resources
# =================================================================
echo "📁 Updating MainActivity.kt with Material Icons..."
cat > app/src/main/java/com/ultrastream/app/MainActivity.kt <<'EOF'
package com.ultrastream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                        Triple(Screen.Home, "Home", Icons.Default.Home),
                        Triple(Screen.Library, "Library", Icons.Default.VideoLibrary),
                        Triple(Screen.Search, "Search", Icons.Default.Search),
                        Triple(Screen.Addons, "Addons", Icons.Default.Extension),
                        Triple(Screen.Profile, "Profile", Icons.Default.Person)
                    )
                    items.forEach { (screen, title, iconVector) ->
                        NavigationBarItem(
                            icon = { Icon(imageVector = iconVector, contentDescription = title) },
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
                            StreamDataHolder.setStream(stream)
                            navController.navigate(Screen.Player.pass(title))
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
# 3. AndroidManifest.xml - Add permission, update configChanges, add PlayerService
# =================================================================
echo "📁 Updating AndroidManifest.xml..."
cat > app/src/main/AndroidManifest.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.ultrastream.app">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" /> <!-- ✅ नया -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:name=".UltraStreamApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.UltraStream"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden" <!-- ✅ updated -->
            android:exported="true"
            android:supportsPictureInPicture="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <!-- ✅ PlayerService -->
        <service
            android:name=".player.PlayerService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

    </application>
</manifest>
EOF
git add app/src/main/AndroidManifest.xml

# =================================================================
# 4. PlayerViewModel.kt - Add MediaSession integration
# =================================================================
echo "📁 Updating PlayerViewModel.kt with MediaSession..."
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
import androidx.media3.session.MediaSession // ✅ नया import
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
    private var mediaSession: MediaSession? = null // ✅ नया वेरिएबल

    fun initializePlayer(context: Context, stream: StreamItem, title: String, externalSubtitle: Subtitle? = null) {
        currentContext = context
        currentStream = stream
        currentTitle = title

        // Volume fix: initialize with current system volume
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

                // ✅ MediaSession बनाएं ताकि नोटिफिकेशन और लॉक-स्क्रीन कंट्रोल्स काम करें
                mediaSession = MediaSession.Builder(context, exoPlayer).build()

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

                val override = TrackSelectionOverride(
                    trackGroup.mediaTrackGroup,
                    listOf(bestIndex)
                )
                val params = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
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

        // ✅ MediaSession को रिलीज़ करें
        mediaSession?.release()
        mediaSession = null
    }
}
EOF
git add app/src/main/java/com/ultrastream/app/ui/screens/player/PlayerViewModel.kt

# =================================================================
# 5. gradle.properties - Create at project root
# =================================================================
echo "📁 Creating gradle.properties..."
cat > gradle.properties <<'EOF'
# UltraStream/gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
kotlin.stdlib.default.dependency=false
EOF
git add gradle.properties

echo "✅ All final missing pieces applied successfully!"
echo "🚀 Now run: git commit -m \"Apply final missing pieces: media3-session, Material Icons, manifest updates, MediaSession, gradle.properties\" && git push -u origin main"
