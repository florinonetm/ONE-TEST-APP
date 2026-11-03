package com.onelabs.oneplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
    var selectedSeries by remember { mutableStateOf<SeriesItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OneBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(seriesList, key = { it.id }) { series ->
                SeriesCard(
                    series = series,
                    onClick = { selectedSeries = series }
                )
            }
        }
    }

    // Series Seasons & Episode picker Sheet
    selectedSeries?.let { series ->
        ModalBottomSheet(
            onDismissRequest = { selectedSeries = null },
            containerColor = OneDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = series.poster,
                        contentDescription = series.title,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(series.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${series.seasonsCount} Seasons · ${series.category}", color = OneRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("EPISODES (SEASON 1)", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(series.episodes) { ep ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = OneCardSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSeries = null
                                    onPlayEpisode(ep, series)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ep.thumbnail,
                                    contentDescription = ep.title,
                                    modifier = Modifier
                                        .size(56.dp, 40.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("E${ep.episodeNumber}: ${ep.title}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(ep.synopsis, color = TextGray, fontSize = 12.sp, maxLines = 1)
                                }
                                Text(ep.duration, color = TextGrayLight, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = OneRed)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SeriesCard(
    series: SeriesItem,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OneCardSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("series_card_${series.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = series.poster,
                contentDescription = series.title,
                modifier = Modifier
                    .size(90.dp, 125.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(series.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(series.rating, color = OneGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(series.year, color = TextGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${series.seasonsCount} Seasons", color = TextGrayLight, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(series.description, color = TextGray, fontSize = 12.sp, maxLines = 2, lineHeight = 16.sp)
            }
        }
    }
}
