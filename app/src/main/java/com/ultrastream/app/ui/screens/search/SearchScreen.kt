package com.ultrastream.app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.ui.components.FilterChipGroup
import com.ultrastream.app.ui.components.PosterCard

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("popular") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it, filter, sort)
            },
            label = { Text("Search movies, series...") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilterChipGroup(
            chips = listOf("All", "Movie", "Series", "Anime", "TV"),
            selected = filter,
            onSelect = {
                filter = it.lowercase()
                viewModel.search(query, filter, sort)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilterChipGroup(
            chips = listOf("Popular", "Rating", "Year"),
            selected = sort,
            onSelect = {
                sort = it.lowercase()
                viewModel.search(query, filter, sort)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.results.size) { index ->
                    val item = uiState.results[index]
                    PosterCard(
                        meta = item,
                        onClick = { onItemClick(item.id, item.type) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
