package com.vylune.music.ui.screens.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.vylune.music.constants.SpotifySpDcKey
import com.vylune.music.spotify.Spotify
import com.vylune.music.spotify.models.SpotifyPlaylist
import com.vylune.music.spotifyimport.SpotifyImportRepository
import com.vylune.music.utils.dataStore
import com.vylune.music.utils.reportException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpotifyLibraryUiState(
  val isAuthenticated: Boolean = false,
  val accountName: String? = null,
  val avatarUrl: String? = null,
  val playlists: List<SpotifyPlaylist> = emptyList(),
  val likedSongsCount: Int? = null,
  val isLoading: Boolean = false,
  val error: String? = null,
)

@HiltViewModel
class LibrarySpotifyViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val spotifyImportRepository: SpotifyImportRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SpotifyLibraryUiState(isLoading = true))
  val uiState: StateFlow<SpotifyLibraryUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      context.dataStore.data
        .map { prefs -> prefs[SpotifySpDcKey].orEmpty() }
        .distinctUntilChanged()
        .collect {
          loadLibrary()
        }
    }
  }

  fun refresh() {
    loadLibrary(forceRefresh = true)
  }

  fun loadLibrary(forceRefresh: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        val session = spotifyImportRepository.restoreSession()
        if (!session.isAuthenticated) {
          _uiState.update {
            it.copy(
              isAuthenticated = false,
              isLoading = false,
              accountName = null,
              avatarUrl = null,
              playlists = emptyList(),
              likedSongsCount = null,
            )
          }
          return@launch
        }

        // Fetch liked songs count
        val likedResult = runCatching { Spotify.likedSongs(limit = 1, offset = 0).getOrNull()?.total }
        val likedCount = likedResult.getOrNull()

        // Fetch user playlists
        val playlistsResult = Spotify.myPlaylists(limit = 50, offset = 0)
        val playlists = playlistsResult.getOrNull()?.items.orEmpty()

        _uiState.update {
          it.copy(
            isAuthenticated = true,
            accountName = session.accountName,
            avatarUrl = session.accountAvatarUrl,
            playlists = playlists,
            likedSongsCount = likedCount,
            isLoading = false,
            error = null,
          )
        }
      } catch (e: Exception) {
        reportException(e)
        _uiState.update {
          it.copy(
            isLoading = false,
            error = e.message ?: "Failed to load Spotify library",
          )
        }
      }
    }
  }
}
