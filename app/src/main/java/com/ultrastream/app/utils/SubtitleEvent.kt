package com.ultrastream.app.utils

import com.ultrastream.app.data.models.Subtitle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SubtitleEvent {
    private val _events = MutableSharedFlow<Subtitle>()
    val events: SharedFlow<Subtitle> = _events.asSharedFlow()

    suspend fun emit(subtitle: Subtitle) {
        _events.emit(subtitle)
    }
}
