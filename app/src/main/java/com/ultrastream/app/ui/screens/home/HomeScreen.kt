package com.ultrastream.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrastream.app.data.models.MetaItem  // ✅ Added
import com.ultrastream.app.ui.components.ContinueWatchingCard
import com.ultrastream.app.ui.components.HScrollRow
import com.ultrastream.app.ui.components.PosterCard
import com.ultrastream.app.ui.components.RecommendedAddonCard
import com.ultrastream.app.ui.components.SectionHeader

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onItemClick: (id: String, type: String) -> Unit,
    onSeeAll: (rowId: String, items: List<MetaItem>, title: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Continue Watching
        item {
            SectionHeader(title = "Continue Watching")
            if (uiState.continueWatching.isEmpty()) {
                Text("No history yet", modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                HScrollRow {
                    uiState.continueWatching.forEach { (history, progress) ->
                        ContinueWatchingCard(
                            history = history,
                            progressPercent = progress,
                            onClick = { onItemClick(history.id, history.type) }
                        )
                    }
                }
            }
        }

        // ✅ RECOMMENDATIONS (Because you watched)
        if (uiState.recommendations.isNotEmpty()) {
            item {
                SectionHeader(title = "🎯 Because you watched")
                HScrollRow {
                    uiState.recommendations.forEach { meta ->
                        PosterCard(
                            meta = meta,
                            onClick = { onItemClick(meta.id, meta.type) }
                        )
                    }
                }
            }
        }

        // Recommended Addons (keep)
        item {
            SectionHeader(title = "Recommended Addons")
            if (uiState.recommendedAddons.isEmpty()) {
                Text("No recommendations", modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                HScrollRow {
                    uiState.recommendedAddons.forEach { addon ->
                        RecommendedAddonCard(
                            addon = addon,
                            onInstall = { url ->
                                // TODO: trigger install via ViewModel
                            }
                        )
                    }
                }
            }
        }

        // Catalog rows with See All
        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            uiState.catalogRows.forEach { (rowId, items) ->
                item {
                    val parts = rowId.split("_")
                    val displayName = when {
                        parts.size >= 3 -> {
                            val type = parts[1]
                            val name = parts.drop(2).joinToString(" ")
                            "$type $name".replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                        else -> "Catalog"
                    }
                    SectionHeader(
                        title = displayName,
                        actionText = "See All",
                        onActionClick = { onSeeAll(rowId, items, displayName) }
                    )
                    if (items.isEmpty()) {
                        Text("No items", modifier = Modifier.padding(horizontal = 16.dp))
                    } else {
                        HScrollRow {
                            items.forEach { meta ->
                                PosterCard(
                                    meta = meta,
                                    onClick = { onItemClick(meta.id, meta.type) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
