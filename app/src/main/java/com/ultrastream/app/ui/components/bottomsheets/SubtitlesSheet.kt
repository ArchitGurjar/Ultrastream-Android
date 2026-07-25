@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components.bottomsheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultrastream.app.data.models.Subtitle

@Composable
fun SubtitlesSheet(
    subtitles: List<Subtitle>,
    onDismiss: () -> Unit,
    onSubtitleSelected: (Subtitle) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Subtitles", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(subtitles.size) { index ->
                    val sub = subtitles[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onSubtitleSelected(sub) }
                    ) {
                        Text(
                            text = sub.name ?: sub.lang ?: "Unknown",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
