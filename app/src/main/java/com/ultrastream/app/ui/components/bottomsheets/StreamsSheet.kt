@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ultrastream.app.ui.components.bottomsheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultrastream.app.data.models.StreamItem
import com.ultrastream.app.ui.components.StreamCard

@Composable
fun StreamsSheet(
    streams: List<StreamItem>,
    onDismiss: () -> Unit,
    onStreamClick: (StreamItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Available Streams", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(streams.size) { index ->
                    val stream = streams[index]
                    StreamCard(
                        stream = stream,
                        onClick = { onStreamClick(stream) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
