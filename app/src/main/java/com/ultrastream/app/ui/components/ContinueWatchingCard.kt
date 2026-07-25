@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ultrastream.app.data.models.HistoryItem

@Composable
fun ContinueWatchingCard(
    history: HistoryItem,
    progressPercent: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = history.poster,
                contentDescription = history.name,
                modifier = Modifier
                    .width(75.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = history.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = history.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (progressPercent > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 4.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPercent / 100f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
