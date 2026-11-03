package com.onelabs.oneplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelabs.oneplayer.data.model.PlaylistItem
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(
    repository: PlaylistRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val playlists by repository.playlists.collectAsState()
    val parsingProgress by repository.parsingProgress.collectAsState()
    val activeUserAgent by repository.userAgentManager.currentUserAgent.collectAsState()

    var showAddM3uDialog by remember { mutableStateOf(false) }
    var showAddXtreamDialog by remember { mutableStateOf(false) }
    var showUaDialog by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OneBlack)
            .padding(16.dp)
    ) {
        // User-Agent & Timeout Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = OneCardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showUaDialog = true }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = OneRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STREAM USER-AGENT (Bypass Blocks)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Change >", color = OneRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activeUserAgent,
                    color = TextGrayLight,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(OneGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Timeout: 180s (3m)", color = OneGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(OneBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("GZIP Fast Load", color = OneBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists & Accounts",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (playlists.isNotEmpty()) {
                Text(
                    text = "${playlists.size} active",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Symmetrical, equal size, no text wrapping
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showAddM3uDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = OneSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("add_m3u_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add M3U",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = { showAddXtreamDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = OneRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("add_xtream_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Xtream",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Parsing progress banner
        parsingProgress?.let { prog ->
            Card(
                colors = CardDefaults.cardColors(containerColor = OneCardSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = OneRed, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(prog, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Result message banner
        importStatusMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = OneSurfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OneGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(msg, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { importStatusMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(playlists, key = { it.id }) { pl ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OneCardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (pl.type == "XTREAM") OneRed.copy(alpha = 0.2f) else OneBlue.copy(alpha = 0.2f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                pl.type,
                                color = if (pl.type == "XTREAM") OneRed else OneBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            val subtitle = if (pl.type == "XTREAM") pl.server else pl.url
                            Text(subtitle, color = TextGray, fontSize = 12.sp, maxLines = 1)
                            Text("${pl.channelCount} Streams Loaded · Updated: ${pl.lastUpdated}", color = TextGrayDark, fontSize = 11.sp)
                        }

                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = OneGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // User-Agent Selector Dialog
    if (showUaDialog) {
        var customUaInput by remember { mutableStateOf(activeUserAgent) }
        AlertDialog(
            onDismissRequest = { showUaDialog = false },
            title = { Text("Select Stream User-Agent", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = OneDarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "IPTV providers often require specific player headers to grant access to live streams and M3U playlists.",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    repository.userAgentManager.presets.forEach { preset ->
                        val isSelected = activeUserAgent == preset.userAgent
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) OneSurfaceVariant else OneCardSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.userAgentManager.setUserAgent(preset.userAgent)
                                    showUaDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = OneRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(preset.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(preset.description, color = TextGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Or enter custom User-Agent:", color = TextGray, fontSize = 11.sp)
                    OutlinedTextField(
                        value = customUaInput,
                        onValueChange = { customUaInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OneRed,
                            unfocusedBorderColor = OneBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.userAgentManager.setUserAgent(customUaInput)
                        showUaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OneRed)
                ) {
                    Text("Apply User-Agent", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUaDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // Add M3U Dialog
    if (showAddM3uDialog) {
        var playlistName by remember { mutableStateOf("") }
        var m3uUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddM3uDialog = false },
            title = { Text("Add M3U Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = OneDarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Loads full playlist with extended 180s timeout and active User-Agent.", color = TextGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name (e.g. Premium IPTV)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text("M3U / M3U8 URL (http://...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddM3uDialog = false
                        coroutineScope.launch {
                            val res = repository.addM3uPlaylist(playlistName, m3uUrl)
                            importStatusMessage = res.second
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OneRed)
                ) {
                    Text("Load Full Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddM3uDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // Add Xtream Dialog
    if (showAddXtreamDialog) {
        var name by remember { mutableStateOf("") }
        var server by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddXtreamDialog = false },
            title = { Text("Connect Xtream Codes API", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = OneDarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text("Server URL (http://host:port)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddXtreamDialog = false
                        coroutineScope.launch {
                            val res = repository.addXtreamAccount(name, server, username, password)
                            importStatusMessage = res.second
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OneRed)
                ) {
                    Text("Connect & Sync", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddXtreamDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }
}
