package com.ultrastream.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ultrastream.app.data.models.MetaItem

@Composable
fun GridSection(
    items: List<MetaItem>,
    onItemClick: (id: String, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chunkedItems = items.chunked(3)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    PosterCard(
                        meta = item,
                        onClick = { onItemClick(item.id, item.type) },
                        modifier = Modifier.weight(1f)
                    )
                }
                val emptySpaces = 3 - rowItems.size
                repeat(emptySpaces) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
