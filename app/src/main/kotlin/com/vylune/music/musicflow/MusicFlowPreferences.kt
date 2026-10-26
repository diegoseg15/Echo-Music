package com.vylune.music.musicflow

import androidx.datastore.preferences.core.Preferences
import com.vylune.music.constants.CanvasThumbnailAnimationKey
import com.vylune.music.constants.DataSaverEnabledKey
import com.vylune.music.constants.MusicFlowAnimatedBackgroundKey
import com.vylune.music.constants.MusicFlowAutoNextKey
import com.vylune.music.constants.MusicFlowAutoplayKey
import com.vylune.music.constants.MusicFlowCellularPrefetchKey
import com.vylune.music.constants.MusicFlowEnabledKey
import com.vylune.music.constants.MusicFlowHideRecentKey
import com.vylune.music.constants.MusicFlowLyricsKey

data class MusicFlowPreferences(
  val enabled: Boolean = true,
  val autoplay: Boolean = true,
  val autoNext: Boolean = true,
  val animatedBackground: Boolean = false,
  val lyrics: Boolean = true,
  val cellularPrefetch: Boolean = true,
  val hideRecent: Boolean = true,
  val dataSaver: Boolean = false,
) {
  companion object {
    fun from(preferences: Preferences) =
      MusicFlowPreferences(
        enabled = preferences[MusicFlowEnabledKey] ?: true,
        autoplay = preferences[MusicFlowAutoplayKey] ?: true,
        autoNext = preferences[MusicFlowAutoNextKey] ?: true,
        animatedBackground =
          preferences[MusicFlowAnimatedBackgroundKey]
            ?: preferences[CanvasThumbnailAnimationKey]
            ?: false,
        lyrics = preferences[MusicFlowLyricsKey] ?: true,
        cellularPrefetch = preferences[MusicFlowCellularPrefetchKey] ?: true,
        hideRecent = preferences[MusicFlowHideRecentKey] ?: true,
        dataSaver = preferences[DataSaverEnabledKey] ?: false,
      )
  }
}
