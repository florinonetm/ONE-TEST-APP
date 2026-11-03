package com.onelabs.oneplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelabs.oneplayer.R
import com.onelabs.oneplayer.data.license.LicenseManager
import com.onelabs.oneplayer.data.model.Channel
import com.onelabs.oneplayer.data.model.EpisodeItem
import com.onelabs.oneplayer.data.model.SeriesItem
import com.onelabs.oneplayer.data.model.VodItem
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.components.FullscreenPlayer
import com.onelabs.oneplayer.ui.screens.*
import com.onelabs.oneplayer.ui.theme.*

enum class OneNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIVE_TV("Live TV", Icons.Default.LiveTv),
    MOVIES("Movies", Icons.Default.Movie),
    SERIES("Series", Icons.Default.VideoLibrary),
    MY_ACCOUNT("My ONE", Icons.Default.Person)
}

data class ActivePlayback(
    val url: String,
    val title: String,
    val category: String,
    val isLive: Boolean,
    val poster: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: PlaylistRepository,
    licenseManager: LicenseManager
) {
    var currentTab by remember { mutableStateOf(OneNavTab.HOME) }
    var activePlayback by remember { mutableStateOf<ActivePlayback?>(null) }
    var showProfilesDialog by remember { mutableStateOf(false) }
    var showPlaylistsDialog by remember { mutableStateOf(false) }

    val licenseStatus by licenseManager.licenseStatus.collectAsState()
    val activeProfile by repository.activeProfile.collectAsState()
    val activeUserAgent by repository.userAgentManager.currentUserAgent.collectAsState()

    // Handle back button when player is active
    BackHandler(enabled = activePlayback != null) {
        activePlayback = null
    }

    Scaffold(
        topBar = {
            if (activePlayback == null) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // ONE Brand App Icon
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_fg_img),
                                contentDescription = "ONE Player",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "ONE",
                                    color = OneRed,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "PLAYER",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick 31d trial pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OneCardSurface,
                            modifier = Modifier
                                .clickable { currentTab = OneNavTab.MY_ACCOUNT }
                                .padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(if (licenseStatus.isTrialActive) OneGreen else OneRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${licenseStatus.daysRemaining}d Trial",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Playlists Quick Access
                        IconButton(
                            onClick = { showPlaylistsDialog = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.PlaylistPlay,
                                contentDescription = "Playlists & Accounts",
                                tint = Color.White
                            )
                        }

                        // Profile Avatar
                        IconButton(
                            onClick = { showProfilesDialog = true },
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OneRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    activeProfile?.name?.take(1) ?: "U",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OneBlack
                    )
                )
            }
        },
        bottomBar = {
            if (activePlayback == null) {
                NavigationBar(
                    containerColor = OneDarkSurface,
                    contentColor = Color.White
                ) {
                    OneNavTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) Color.White else TextGrayDark
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    color = if (selected) Color.White else TextGrayDark,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = OneSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = OneBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                OneNavTab.HOME -> HomeScreen(
                    repository = repository,
                    onPlayChannel = { ch: Channel ->
                        activePlayback = ActivePlayback(
                            url = ch.streamUrl,
                            title = ch.name,
                            category = ch.category,
                            isLive = true,
                            poster = ch.logo
                        )
                    },
                    onPlayVod = { vod: VodItem ->
                        activePlayback = ActivePlayback(
                            url = vod.streamUrl,
                            title = vod.title,
                            category = "${vod.category} · ${vod.year}",
                            isLive = false,
                            poster = vod.poster
                        )
                    },
                    onPlaySeries = { ep: EpisodeItem, series: SeriesItem ->
                        activePlayback = ActivePlayback(
                            url = ep.streamUrl,
                            title = "${series.title} - S${ep.seasonNumber}E${ep.episodeNumber}: ${ep.title}",
                            category = series.category,
                            isLive = false,
                            poster = series.poster
                        )
                    },
                    onOpenLiveTv = { currentTab = OneNavTab.LIVE_TV },
                    onOpenMovies = { currentTab = OneNavTab.MOVIES },
                    onOpenSeries = { currentTab = OneNavTab.SERIES }
                )
                OneNavTab.LIVE_TV -> LiveTvScreen(
                    repository = repository,
                    onPlayChannel = { ch: Channel ->
                        activePlayback = ActivePlayback(
                            url = ch.streamUrl,
                            title = ch.name,
                            category = ch.category,
                            isLive = true,
                            poster = ch.logo
                        )
                    }
                )
                OneNavTab.MOVIES -> VodScreen(
                    repository = repository,
                    onPlayVod = { vod: VodItem ->
                        activePlayback = ActivePlayback(
                            url = vod.streamUrl,
                            title = vod.title,
                            category = "${vod.category} · ${vod.year}",
                            isLive = false,
                            poster = vod.poster
                        )
                    }
                )
                OneNavTab.SERIES -> SeriesScreen(
                    repository = repository,
                    onPlayEpisode = { ep: EpisodeItem, series: SeriesItem ->
                        activePlayback = ActivePlayback(
                            url = ep.streamUrl,
                            title = "${series.title} - S${ep.seasonNumber}E${ep.episodeNumber}: ${ep.title}",
                            category = series.category,
                            isLive = false,
                            poster = series.poster.ifEmpty { ep.thumbnail }
                        )
                    }
                )
                OneNavTab.MY_ACCOUNT -> MyAccountScreen(
                    licenseManager = licenseManager,
                    repository = repository
                )
            }

            // Fullscreen player overlay with User-Agent
            activePlayback?.let { pb ->
                FullscreenPlayer(
                    streamUrl = pb.url,
                    title = pb.title,
                    category = pb.category,
                    isLive = pb.isLive,
                    userAgent = activeUserAgent,
                    onClose = { lastPos, dur ->
                        if (dur > 0) {
                            val frac = (lastPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                            repository.updateContinueWatching(
                                title = pb.title,
                                url = pb.url,
                                poster = pb.poster,
                                progress = frac,
                                positionMs = lastPos,
                                isLive = pb.isLive
                            )
                        }
                        activePlayback = null
                    }
                )
            }
        }
    }

    // Profiles Bottom Sheet Modal
    if (showProfilesDialog) {
        ModalBottomSheet(
            onDismissRequest = { showProfilesDialog = false },
            containerColor = OneDarkSurface
        ) {
            ProfilesScreen(repository = repository)
        }
    }

    // Playlists Bottom Sheet Modal
    if (showPlaylistsDialog) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistsDialog = false },
            containerColor = OneBlack
        ) {
            PlaylistsScreen(
                repository = repository,
                onClose = { showPlaylistsDialog = false }
            )
        }
    }
}

@Composable
fun MyAccountScreen(
    licenseManager: LicenseManager,
    repository: PlaylistRepository
) {
    var selectedSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OneBlack)
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = OneDarkSurface,
            contentColor = Color.White
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Device Pairing", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Playlists & M3U", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Diagnostics", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        when (selectedSubTab) {
            0 -> DevicePairingScreen(licenseManager = licenseManager)
            1 -> PlaylistsScreen(repository = repository)
            2 -> DiagnosticsScreen()
        }
    }
}
