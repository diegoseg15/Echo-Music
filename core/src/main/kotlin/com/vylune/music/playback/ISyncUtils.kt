package com.vylune.music.playback

import com.vylune.music.db.entities.SongEntity

interface ISyncUtils {
  fun likeSong(song: SongEntity)
}
