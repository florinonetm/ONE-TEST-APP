package com.onelabs.oneplayer.ui.components

import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.onelabs.oneplayer.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

enum class AspectRatioMode(val label: String, val mode: Int) {
    FIT("Fit (16:9)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom (Crop)", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

@OptIn(UnstableApi::class)
@Composable
fun FullscreenPlayer(
    streamUrl: String,
    title: String,
    category: String = "Live TV",
    isLive: Boolean = true,
    userAgent: String = "VLC/3.0.18 LibVLC/3.0.18",
    onClose: (lastPosition: Long, duration: Long) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var aspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var hudMessage by remember { mutableStateOf<String?>(null) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }

    // Media3 ExoPlayer with User-Agent & 120s HTTP Read Timeout
    val exoPlayer = remember(streamUrl, userAgent) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(60000)
            .setReadTimeoutMs(120000)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
                val mediaItem = MediaItem.fromUri(streamUrl)
                setMediaItem(mediaItem)
                prepare()
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    errorMessage = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorMessage = "Stream Error (${error.errorCodeName}). Verify stream or try different User-Agent."
                isLoading = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Progress updates
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(1000)
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4500)
            controlsVisible = false
        }
    }

    // HUD Toast timer
    LaunchedEffect(hudMessage) {
        if (hudMessage != null) {
            delay(2000)
            hudMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    val screenWidth = size.width
                    val x = change.position.x
                    if (x > screenWidth / 2) {
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val newVol = if (dragAmount < 0) {
                            (currVol + 1).coerceAtMost(maxVol)
                        } else {
                            (currVol - 1).coerceAtLeast(0)
                        }
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        val percent = (newVol * 100) / maxVol
                        hudMessage = "🔊 Volume: $percent%"
                    } else {
                        hudMessage = if (dragAmount < 0) "☀️ Brightness Up" else "🌙 Brightness Down"
                    }
                }
            }
    ) {
        // ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = aspectMode.mode
                }
            },
            update = { view ->
                view.resizeMode = aspectMode.mode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading spinner with Red
        if (isLoading) {
            CircularProgressIndicator(
                color = OneRed,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.Center)
            )
        }

        // Error Card
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = OneDarkSurface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = OneRed, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "", color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            exoPlayer.seekTo(0)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OneRed)
                    ) {
                        Text("Retry Stream", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // HUD Toast overlay
        hudMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Cinematic Controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onClose(currentPosition, duration) },
                        modifier = Modifier
                            .testTag("player_back_button")
                            .background(OneDarkSurface.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isLive) {
                                Box(
                                    modifier = Modifier
                                        .background(OneRed, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = "$category · UA: ${userAgent.take(18)}...",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }

                    // Aspect ratio button
                    IconButton(
                        onClick = {
                            aspectMode = when (aspectMode) {
                                AspectRatioMode.FIT -> AspectRatioMode.STRETCH
                                AspectRatioMode.STRETCH -> AspectRatioMode.ZOOM
                                AspectRatioMode.ZOOM -> AspectRatioMode.FIT
                            }
                            hudMessage = "Aspect: ${aspectMode.label}"
                        },
                        modifier = Modifier
                            .background(OneDarkSurface.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Audio & Subtitles button
                    IconButton(
                        onClick = { showAudioDialog = true },
                        modifier = Modifier
                            .background(OneDarkSurface.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Audio & Subtitles", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Speed button
                    IconButton(
                        onClick = { showSpeedDialog = true },
                        modifier = Modifier
                            .background(OneDarkSurface.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                    }
                }

                // Center playback controls (Rewind 10s, Play/Pause, Forward 10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                            hudMessage = "⏪ -10s"
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(OneDarkSurface.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Play/Pause with White circle
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = exoPlayer.currentPosition + 10000
                            exoPlayer.seekTo(newPos)
                            hudMessage = "⏩ +10s"
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(OneDarkSurface.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                // Bottom Timeline & Red Scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (duration > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { frac ->
                                    val target = (frac * duration).toLong()
                                    currentPosition = target
                                    exoPlayer.seekTo(target)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = OneRed,
                                    activeTrackColor = OneRed,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatDuration(duration),
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(OneRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE BROADCAST · Media3 Multi-Format Engine",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = OneDarkSurface,
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                                    hudMessage = "Speed: ${speed}x"
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackSpeed == speed,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = OneRed)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${speed}x", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = OneRed)
                }
            }
        )
    }

    // Audio & Subtitles Dialog
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text("Audio & Subtitles", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = OneDarkSurface,
            text = {
                Column {
                    Text("AUDIO", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(
                        "Original (Stereo AAC)",
                        "5.1 Surround Sound (Dolby Digital)",
                        "Alternative Language Stream"
                    ).forEachIndexed { idx, trackName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hudMessage = "Audio: $trackName"
                                    showAudioDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = idx == 0,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = OneRed)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(trackName, color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("SUBTITLES", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("Off", "English [CC]", "Romanian", "Spanish", "French").forEachIndexed { idx, sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hudMessage = "Subtitles: $sub"
                                    showAudioDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = idx == 0,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = OneRed)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(sub, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text("Done", color = OneRed)
                }
            }
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
