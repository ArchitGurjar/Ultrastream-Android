package com.ultrastream.app.utils

import com.ultrastream.app.data.models.StreamItem

object StreamDataHolder {
    var currentStream: StreamItem? = null
        private set

    fun setStream(stream: StreamItem) {
        currentStream = stream
    }

    fun clear() {
        currentStream = null
    }
}
