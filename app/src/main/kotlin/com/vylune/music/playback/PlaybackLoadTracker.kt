package com.vylune.music.playback

import androidx.media3.common.MediaItem
import com.vylune.music.utils.LoadTimeLogger

internal class PlaybackLoadTracker {
    private var playbackLoadMediaId: String? = null

    fun markLoadStart(
        reason: String,
        mediaItem: MediaItem?,
    ) {
        val mediaId = mediaItem?.mediaId?.takeIf { it.isNotBlank() } ?: return

        if (playbackLoadMediaId == mediaId) {
            return
        }

        playbackLoadMediaId = mediaId

        LoadTimeLogger.start(
            key = "player:$mediaId",
            stage = "player_load",
            mediaId = mediaId,
            extra = "reason=$reason",
        )
    }

    fun markReady(mediaItem: MediaItem?) {
        val mediaId =
            mediaItem?.mediaId?.takeIf { it.isNotBlank() }
                ?: playbackLoadMediaId
                ?: return

        LoadTimeLogger.end(
            key = "player:$mediaId",
            stage = "player_ready",
            mediaId = mediaId,
        )

        playbackLoadMediaId = null
    }
}