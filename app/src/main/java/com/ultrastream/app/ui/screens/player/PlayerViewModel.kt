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
