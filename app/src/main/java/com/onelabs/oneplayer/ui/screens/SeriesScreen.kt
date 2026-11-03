package com.onelabs.oneplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onelabs.oneplayer.data.model.EpisodeItem
import com.onelabs.oneplayer.data.model.SeriesItem
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesScreen(
    repository: PlaylistRepository,
    onPlayEpisode: (EpisodeItem, SeriesItem) -> Unit
) {
    val seriesList by repository.seriesItems.collectAsState()
    val favorites by repository.favorites.collectAsState()

    var selectedCategory by remember { mutableStateOf("All Series") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSeries by remember { mutableStateOf<SeriesItem?>(null) }
    var selectedSeason by remember { mutableIntStateOf(1) }

    // Dynamic Categories
    val categories = remember(seriesList) {
        val cats = seriesList.map { it.category }.distinct()
        listOf("All Series", "My List") + cats
    }

    // Filtered Series
    val filteredSeries = remember(seriesList, selectedCategory, searchQuery, favorites) {
        seriesList.filter { series ->
            val matchesCategory = when (selectedCategory) {
                "All Series" -> true
                "My List" -> favorites.contains(series.id)
                else -> series.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    series.title.contains(searchQuery, ignoreCase = true) ||
                    series.category.contains(searchQuery, ignoreCase = true) ||
                    series.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Featured Series for Hero Banner
    val featuredSeries = remember(seriesList) {
        seriesList.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OneBlack)
    ) {
        // 1. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search series, actors, genres...", color = TextGrayDark) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = OneRed) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextGray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OneRed,
                unfocusedBorderColor = OneBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = OneDarkSurface,
                unfocusedContainerColor = OneDarkSurface
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("series_search_input")
        )

        // 2. Categories Horizontal Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) OneRed else OneCardSurface,
                    modifier = Modifier
                        .clickable { selectedCategory = category }
                        .testTag("series_filter_${category.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category == "My List") {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else OneRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = category,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Main Series Content (Hero + Grid)
        if (filteredSeries.isEmpty()) {
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
                        tint = TextGray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedCategory == "My List") "No series saved in My List" else "No series found",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try searching for a different title or select All Series.",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = "All Series"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OneRed),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Reset Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Optional Hero Banner when on "All Series" and no search query
                if (searchQuery.isEmpty() && selectedCategory == "All Series" && featuredSeries != null) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        SeriesHeroCard(
                            series = featuredSeries,
                            isFavorite = favorites.contains(featuredSeries.id),
                            onPlayFirstEpisode = {
                                val firstEp = featuredSeries.episodes.firstOrNull()
                                if (firstEp != null) {
                                    onPlayEpisode(firstEp, featuredSeries)
                                } else {
                                    selectedSeries = featuredSeries
                                    selectedSeason = 1
                                }
                            },
                            onOpenEpisodes = {
                                selectedSeries = featuredSeries
                                selectedSeason = 1
                            },
                            onToggleFavorite = {
                                repository.toggleFavorite(featuredSeries.id)
                            }
                        )
                    }
                }

                // Grid of Series Poster Cards
                items(filteredSeries, key = { it.id }) { series ->
                    SeriesGridCard(
                        series = series,
                        isFavorite = favorites.contains(series.id),
                        onClick = {
                            selectedSeries = series
                            val seasons = repository.getSeriesSeasons(series)
                            selectedSeason = seasons.firstOrNull() ?: 1
                        },
                        onToggleFavorite = {
                            repository.toggleFavorite(series.id)
                        }
                    )
                }
            }
        }
    }

    // 4. Series Seasons & Episodes Modal Bottom Sheet
    selectedSeries?.let { series ->
        val seasons = remember(series) { repository.getSeriesSeasons(series) }
        val episodesForSeason = remember(series, selectedSeason) {
            repository.getEpisodesForSeason(series, selectedSeason)
        }
        val isFav = favorites.contains(series.id)

        ModalBottomSheet(
            onDismissRequest = { selectedSeries = null },
            containerColor = OneDarkSurface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Details Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        AsyncImage(
                            model = series.poster,
                            contentDescription = series.title,
                            modifier = Modifier
                                .size(88.dp, 126.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = series.title,
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = series.rating,
                                    color = OneGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = series.year,
                                    color = TextGrayLight,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(OneSurfaceVariant, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("4K UHD", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${series.seasonsCount} Seasons · ${series.category}",
                                color = OneRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val epToPlay = episodesForSeason.firstOrNull() ?: series.episodes.firstOrNull()
                                        if (epToPlay != null) {
                                            selectedSeries = null
                                            onPlayEpisode(epToPlay, series)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OneRed),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play S${selectedSeason}:E1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { repository.toggleFavorite(series.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFav) OneRed else OneBorder),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        if (isFav) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isFav) OneRed else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isFav) "In My List" else "My List", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Description
                item {
                    Text(
                        text = series.description,
                        color = TextGrayLight,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                // Season Selector
                item {
                    Column {
                        Text(
                            text = "SEASONS",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(seasons) { seasonNum ->
                                val isSeasonSelected = seasonNum == selectedSeason
                                val seasonEpCount = series.episodes.count { it.seasonNumber == seasonNum }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSeasonSelected) OneRed else OneCardSurface,
                                    modifier = Modifier.clickable { selectedSeason = seasonNum }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Season $seasonNum",
                                            color = Color.White,
                                            fontWeight = if (isSeasonSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        if (seasonEpCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "($seasonEpCount)",
                                                color = if (isSeasonSelected) Color.White.copy(alpha = 0.8f) else TextGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Episode List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EPISODES · SEASON $selectedSeason",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${episodesForSeason.size} Episodes",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Episodes
                items(episodesForSeason, key = { it.id }) { ep ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSeries = null
                                onPlayEpisode(ep, series)
                            }
                            .testTag("series_episode_${ep.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail with play button overlay
                            Box(
                                modifier = Modifier
                                    .size(80.dp, 56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    model = ep.thumbnail.ifEmpty { series.poster },
                                    contentDescription = ep.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "E${ep.episodeNumber}: ${ep.title}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = ep.synopsis,
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    lineHeight = 15.sp,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = ep.duration,
                                    color = TextGrayLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Play Episode",
                                    tint = OneRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesHeroCard(
    series: SeriesItem,
    isFavorite: Boolean,
    onPlayFirstEpisode: () -> Unit,
    onOpenEpisodes: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onOpenEpisodes() }
            .testTag("series_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = series.poster,
                contentDescription = series.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Badges & details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(OneRed, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SERIES SPOTLIGHT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(series.rating, color = OneGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("· ${series.seasonsCount} Seasons", color = Color.White, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = series.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayFirstEpisode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play S1:E1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = onOpenEpisodes,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = OneSurfaceVariant),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .weight(1f)
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Episodes", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(38.dp)
                            .background(OneSurfaceVariant, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) OneRed else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesGridCard(
    series: SeriesItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("series_card_${series.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                AsyncImage(
                    model = series.poster,
                    contentDescription = series.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top right Season badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${series.seasonsCount} S",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top left Bookmark icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) OneRed else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Bottom gradient for card title contrast
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = series.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = series.category,
                        color = TextGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = series.rating,
                        color = OneGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
