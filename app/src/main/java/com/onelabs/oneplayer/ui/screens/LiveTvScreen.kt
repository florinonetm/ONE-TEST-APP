package com.onelabs.oneplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onelabs.oneplayer.data.model.Channel
import com.onelabs.oneplayer.data.model.EpgProgram
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    repository: PlaylistRepository,
    onPlayChannel: (Channel) -> Unit
) {
    val channels by repository.channels.collectAsState()
    val favorites by repository.favorites.collectAsState()

    var selectedCategory by remember { mutableStateOf("All Channels") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEpgChannel by remember { mutableStateOf<Channel?>(null) }

    val categories = remember(channels) {
        listOf("All Channels", "Favorites") + channels.map { it.category }.distinct()
    }

    val filteredChannels = remember(channels, selectedCategory, searchQuery, favorites) {
        channels.filter { ch ->
            val matchesCategory = when (selectedCategory) {
                "All Channels" -> true
                "Favorites" -> favorites.contains(ch.id)
                else -> ch.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isEmpty() ||
                    ch.name.contains(searchQuery, ignoreCase = true) ||
                    (ch.currentProgram?.contains(searchQuery, ignoreCase = true) == true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search live channels & programs...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("live_tv_search_input")
        )

        // Categories pill carousel
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) NeonCyan else DarkSurfaceVariant,
                    modifier = Modifier
                        .clickable { selectedCategory = category }
                        .testTag("category_pill_${category.replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category == "Favorites") {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSelected) DarkBackground else AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = category,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Channels list
        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TvOff,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No channels found in $selectedCategory",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        isFavorite = favorites.contains(channel.id),
                        onToggleFavorite = { repository.toggleFavorite(channel.id) },
                        onShowEpg = { selectedEpgChannel = channel },
                        onPlay = { onPlayChannel(channel) }
                    )
                }
            }
        }
    }

    // EPG Details Bottom Sheet
    selectedEpgChannel?.let { ch ->
        val epgList = remember(ch) { repository.getEpgForChannel(ch) }
        ModalBottomSheet(
            onDismissRequest = { selectedEpgChannel = null },
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ch.logo.ifEmpty { "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=200" },
                        contentDescription = ch.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(ch.category, color = NeonCyan, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            selectedEpgChannel = null
                            onPlayChannel(ch)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("ELECTRONIC PROGRAM GUIDE (EPG)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                epgList.forEach { program ->
                    EpgItemRow(program = program)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShowEpg: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("channel_card_${channel.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Channel Logo
                AsyncImage(
                    model = channel.logo.ifEmpty { "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=200" },
                    contentDescription = channel.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = channel.currentProgram ?: "Live Broadcast",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                // Info / EPG button
                IconButton(
                    onClick = onShowEpg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Program Guide",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AmberGold else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Program progress bar
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { channel.programProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = NeonCyan,
                trackColor = DarkSurfaceVariant
            )

            if (channel.nextProgram != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next: ${channel.nextProgram}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .background(DangerRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE", color = DangerRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgItemRow(program: EpgProgram) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (program.isCurrent) DarkSurfaceVariant else DarkSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(70.dp)) {
                Text(program.startTime, color = if (program.isCurrent) NeonCyan else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(program.endTime, color = TextMuted, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(program.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (program.isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(NeonCyan, CircleShape)
                                .size(6.dp)
                        )
                    }
                }
                Text(program.description, color = TextSecondary, fontSize = 12.sp, maxLines = 2)
            }
        }
    }
}
