@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ultrastream.app.data.models.SmartPlaylist

@Composable
fun SmartPlaylistCard(
    playlist: SmartPlaylist,
    onClick: () -> Unit,
    onExportM3u: (SmartPlaylist) -> Unit,
    onPlayAll: (SmartPlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = playlist.poster,
                contentDescription = playlist.metaName,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playlist.metaName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Season ${playlist.season}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Progress: ${playlist.fetched}/${playlist.total}",
                    style = MaterialTheme.typography.labelSmall
                )
                val progress = if (playlist.total > 0) playlist.fetched.toFloat() / playlist.total else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (progress == 1f) Color.Green else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = playlist.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (playlist.status == "Complete") Color.Green else Color.Yellow
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPlayAll(playlist) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play All", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onExportM3u(playlist) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export M3U", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("M3U", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
