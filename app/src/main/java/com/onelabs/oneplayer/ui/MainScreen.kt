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

enum class NavTab(val label: String, val icon: ImageVector) {
    LIVE_TV("Live TV", Icons.Default.Tv),
    MOVIES("Movies", Icons.Default.Movie),
    SERIES("Series", Icons.Default.VideoLibrary),
    PLAYLISTS("Playlists", Icons.Default.PlaylistPlay),
    PAIRING("Pairing", Icons.Default.Devices),
    DIAGNOSTICS("Tools", Icons.Default.Speed)
}

data class ActivePlayback(
    val url: String,
    val title: String,
    val category: String,
    val isLive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: PlaylistRepository,
    licenseManager: LicenseManager
) {
    var currentTab by remember { mutableStateOf(NavTab.LIVE_TV) }
    var activePlayback by remember { mutableStateOf<ActivePlayback?>(null) }
    var showProfilesDialog by remember { mutableStateOf(false) }

    val licenseStatus by licenseManager.licenseStatus.collectAsState()
    val activeProfile by repository.activeProfile.collectAsState()

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
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_fg_img),
                                contentDescription = "ONE Player",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "ONE™",
                                        color = NeonCyan,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Player",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "IPTV & Media3 Multi-Format",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    },
                    actions = {
                        // Pairing status pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier
                                .clickable { currentTab = NavTab.PAIRING }
                                .padding(end = 8.dp)
                                .testTag("topbar_pairing_status")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(if (licenseStatus.isTrialActive) NeonLime else DangerRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${licenseStatus.daysRemaining}d Trial",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Profile Avatar
                        IconButton(
                            onClick = { showProfilesDialog = true },
                            modifier = Modifier
                                .testTag("topbar_profile_button")
                                .padding(end = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(NeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    activeProfile?.name?.take(1) ?: "U",
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground
                    )
                )
            }
        },
        bottomBar = {
            if (activePlayback == null) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = Color.White
                ) {
                    NavTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) NeonCyan else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    color = if (selected) NeonCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.LIVE_TV -> LiveTvScreen(
                    repository = repository,
                    onPlayChannel = { ch: Channel ->
                        activePlayback = ActivePlayback(
                            url = ch.streamUrl,
                            title = ch.name,
                            category = ch.category,
                            isLive = true
                        )
                    }
                )
                NavTab.MOVIES -> VodScreen(
                    repository = repository,
                    onPlayVod = { vod: VodItem ->
                        activePlayback = ActivePlayback(
                            url = vod.streamUrl,
                            title = vod.title,
                            category = "${vod.category} · ${vod.year}",
                            isLive = false
                        )
                    }
                )
                NavTab.SERIES -> SeriesScreen(
                    repository = repository,
                    onPlayEpisode = { ep: EpisodeItem, series: SeriesItem ->
                        activePlayback = ActivePlayback(
                            url = ep.streamUrl,
                            title = "${series.title} - S${ep.seasonNumber}E${ep.episodeNumber}: ${ep.title}",
                            category = series.category,
                            isLive = false
                        )
                    }
                )
                NavTab.PLAYLISTS -> PlaylistsScreen(
                    repository = repository
                )
                NavTab.PAIRING -> DevicePairingScreen(
                    licenseManager = licenseManager
                )
                NavTab.DIAGNOSTICS -> DiagnosticsScreen()
            }

            // Fullscreen player overlay
            activePlayback?.let { pb ->
                FullscreenPlayer(
                    streamUrl = pb.url,
                    title = pb.title,
                    category = pb.category,
                    isLive = pb.isLive,
                    onClose = { activePlayback = null }
                )
            }
        }
    }

    // Profiles Bottom Sheet Modal
    if (showProfilesDialog) {
        ModalBottomSheet(
            onDismissRequest = { showProfilesDialog = false },
            containerColor = DarkSurface
        ) {
            ProfilesScreen(repository = repository)
        }
    }
}
