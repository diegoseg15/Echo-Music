package com.vylune.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.pages.BrowseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.vylune.music.constants.HideExplicitKey
import com.vylune.music.constants.HideVideoSongsKey
import com.vylune.music.constants.HideYoutubeShortsKey
import com.vylune.music.utils.dataStore
import com.vylune.music.utils.get
import com.vylune.music.utils.reportException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
  @ApplicationContext val context: Context,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val browseId = savedStateHandle.get<String>("browseId")!!
  private val params = savedStateHandle.get<String>("params")

  val result = MutableStateFlow<BrowseResult?>(null)

  init {
    viewModelScope.launch {
      val hideExplicit = context.dataStore.get(HideExplicitKey, false)
      val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
      val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
      YouTube.browse(browseId, params)
        .onSuccess {
          result.value =
            it
              .filterExplicit(hideExplicit)
              .filterVideoSongs(hideVideoSongs)
              .filterYoutubeShorts(hideYoutubeShorts)
        }
        .onFailure { reportException(it) }
    }
  }
}
