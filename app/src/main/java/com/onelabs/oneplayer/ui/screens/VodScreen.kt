package com.onelabs.oneplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onelabs.oneplayer.data.model.VodItem
import com.onelabs.oneplayer.data.repository.PlaylistRepository
import com.onelabs.oneplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VodScreen(
    repository: PlaylistRepository,
    onPlayVod: (VodItem) -> Unit
) {
    val vodList by repository.vodItems.collectAsState()
    var selectedCategory by remember { mutableStateOf("All Movies") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedVod by remember { mutableStateOf<VodItem?>(null) }

    val categories = remember(vodList) {
        listOf("All Movies") + vodList.map { it.category }.distinct()
    }

    val filteredVod = remember(vodList, selectedCategory, searchQuery) {
        vodList.filter { item ->
            val matchesCategory = selectedCategory == "All Movies" || item.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OneBlack)
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search movies, cast, genres...", color = TextGrayDark) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = OneRed) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OneRed,
                unfocusedBorderColor = OneBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = OneDarkSurface,
                unfocusedContainerColor = OneDarkSurface
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("vod_search_input")
        )

        // Categories
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
                    color = if (isSelected) OneRed else OneCardSurface,
                    modifier = Modifier.clickable { selectedCategory = category }
                ) {
                    Text(
                        text = category,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Movies 3-column poster grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredVod, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OneCardSurface),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clickable { selectedVod = item }
                        .testTag("vod_card_${item.id}")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = item.poster,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(item.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    // Movie Detail Modal Sheet
    selectedVod?.let { movie ->
        ModalBottomSheet(
            onDismissRequest = { selectedVod = null },
            containerColor = OneDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Backdrop banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = movie.backdrop.ifEmpty { movie.poster },
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, OneDarkSurface.copy(alpha = 0.95f))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(movie.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(movie.rating, color = OneGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(movie.year, color = TextGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(OneCardSurface, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("16+", color = TextGrayLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .background(OneCardSurface, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("4K Ultra HD", color = TextGrayLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Play Button
                Button(
                    onClick = {
                        selectedVod = null
                        onPlayVod(movie)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("vod_play_now_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add to My List Button
                OutlinedButton(
                    onClick = { repository.toggleFavorite(movie.id) },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("My List", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(movie.description, color = TextGrayLight, fontSize = 13.sp, lineHeight = 18.sp)

                if (movie.cast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Starring: ${movie.cast}", color = TextGrayDark, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
