package com.onelabs.oneplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onelabs.oneplayer.data.model.Channel
import com.onelabs.oneplayer.data.model.EpisodeItem
import com.onelabs.oneplayer.data.model.SeriesItem
import com.onelabs.oneplayer.data.model.VodItem
import com.onelabs.oneplayer.data.repository.ContinueWatchingItem
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.theme.*

@Composable
fun HomeScreen(
    repository: PlaylistRepository,
    onPlayChannel: (Channel) -> Unit,
    onPlayVod: (VodItem) -> Unit,
    onPlaySeries: (EpisodeItem, SeriesItem) -> Unit,
    onOpenLiveTv: () -> Unit,
    onOpenMovies: () -> Unit
) {
    val channels by repository.channels.collectAsState()
    val vodList by repository.vodItems.collectAsState()
    val seriesList by repository.seriesItems.collectAsState()
    val continueWatching by repository.continueWatching.collectAsState()
    val activeProfile by repository.activeProfile.collectAsState()

    val featuredMovie = remember(vodList) {
        vodList.firstOrNull() ?: VodItem(
            id = "feat",
            title = "Big Buck Bunny (4K Ultra HD)",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            poster = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=500",
            backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200",
            category = "Action & Comedy",
            rating = "98% Match",
            year = "2024",
            duration = "10m",
            description = "A giant, lovable rabbit battles forest bullies in high-speed, breathtaking 4K.",
            cast = "Blender Open Creative Project"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OneBlack),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Hero Billboard Item
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                // Large cinematic backdrop
                AsyncImage(
                    model = featuredMovie.backdrop.ifEmpty { featuredMovie.poster },
                    contentDescription = featuredMovie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Cinematic Gradient Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    OneBlack.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    OneBlack.copy(alpha = 0.8f),
                                    OneBlack
                                )
                            )
                        )
                )

                // Billboard Details & Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top 10 Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(OneRed, RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("TOP 10", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "#1 in Streams Today",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Title
                    Text(
                        text = featuredMovie.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tags
                    Text(
                        text = "${featuredMovie.category} · 4K Ultra HD · Spatial Audio · ${featuredMovie.rating}",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons: Play and My List
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White Play Button
                        Button(
                            onClick = { onPlayVod(featuredMovie) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .weight(1f)
                                .testTag("hero_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // My List Button
                        FilledTonalButton(
                            onClick = { repository.toggleFavorite(featuredMovie.id) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = OneCardSurface.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .weight(1f)
                                .testTag("hero_my_list_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My List", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Row 1: Continue Watching
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeader("Continue Watching for ${activeProfile?.name ?: "You"}")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(continueWatching, key = { it.id }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = {
                                if (item.isLive) {
                                    channels.find { it.streamUrl == item.streamUrl }?.let { onPlayChannel(it) }
                                } else {
                                    vodList.find { it.streamUrl == item.streamUrl }?.let { onPlayVod(it) }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Row 2: Trending Live TV Channels (16:9 Widescreen cards with LIVE indicator)
        if (channels.isNotEmpty()) {
            item {
                SectionHeader("Popular Live Channels", onSeeAll = onOpenLiveTv)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(channels, key = { it.id }) { ch ->
                        LiveChannelCard(channel = ch, onClick = { onPlayChannel(ch) })
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Row 3: Top 10 Today (Ranked 1 to 5 with huge numbers)
        if (vodList.isNotEmpty()) {
            item {
                SectionHeader("Top 10 Today in Movies", onSeeAll = onOpenMovies)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(vodList.take(10)) { index, item ->
                        Top10MovieCard(
                            rank = index + 1,
                            item = item,
                            onClick = { onPlayVod(item) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Row 4: Critically Acclaimed TV Series
        if (seriesList.isNotEmpty()) {
            item {
                SectionHeader("Binge-Worthy TV Series")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(seriesList, key = { it.id }) { series ->
                        SeriesPosterCard(
                            series = series,
                            onClick = {
                                series.episodes.firstOrNull()?.let { ep ->
                                    onPlaySeries(ep, series)
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Row 5: Action & Sci-Fi Blockbusters
        if (vodList.size > 1) {
            item {
                SectionHeader("Action & Sci-Fi Movies")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vodList.drop(1), key = { it.id }) { item ->
                        StandardMovieCard(item = item, onClick = { onPlayVod(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAll != null) {
            Text(
                text = "See All >",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Center play icon overlay
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Red Progress Bar
            LinearProgressIndicator(
                progress = { item.progressFraction },
                color = OneRed,
                trackColor = Color.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun LiveChannelCard(
    channel: Channel,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
            .testTag("home_live_channel_${channel.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = channel.logo.ifEmpty { "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=400" },
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                // LIVE Badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(OneRed, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }

                // Channel name over bottom
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }

            // Live progress
            LinearProgressIndicator(
                progress = { channel.programProgress },
                color = OneRed,
                trackColor = Color.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )

            Text(
                text = channel.currentProgram ?: "Live Broadcast",
                color = TextGrayLight,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun Top10MovieCard(
    rank: Int,
    item: VodItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(170.dp)
            .height(180.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.Bottom
    ) {
        // Giant Rank Number
        Text(
            text = rank.toString(),
            color = Color.White,
            fontSize = 80.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .offset(x = 12.dp, y = 14.dp)
        )

        // Poster
        Card(
            colors = CardDefaults.cardColors(containerColor = OneCardSurface),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .width(115.dp)
                .height(170.dp)
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun SeriesPosterCard(
    series: SeriesItem,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(125.dp)
            .height(185.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = series.poster,
                contentDescription = series.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("${series.seasonsCount} S", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StandardMovieCard(
    item: VodItem,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.poster,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
