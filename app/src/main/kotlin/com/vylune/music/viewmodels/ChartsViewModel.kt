package com.vylune.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.pages.ChartsPage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChartsViewModel @Inject constructor() : ViewModel() {
  private val _chartsPage = MutableStateFlow<ChartsPage?>(null)
  val chartsPage = _chartsPage.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error = _error.asStateFlow()

  fun loadCharts() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null

      val chartsResult = YouTube.getChartsPage().getOrNull()
      if (chartsResult != null && chartsResult.sections.isNotEmpty()) {
        _chartsPage.value = chartsResult
      } else {
        // Fallback 1: Extract sections from YouTube.home()
        val homeResult = YouTube.home().getOrNull()
        val fallbackSections = mutableListOf<ChartsPage.ChartSection>()
        homeResult?.sections?.forEach { section ->
          val songs = section.items.filterIsInstance<SongItem>()
          if (songs.isNotEmpty()) {
            val chartType = when {
              section.title.contains("trend", ignoreCase = true) ||
                section.title.contains("hit", ignoreCase = true) -> ChartsPage.ChartType.TRENDING
              section.title.contains("top", ignoreCase = true) ||
                section.title.contains("week", ignoreCase = true) ||
                section.title.contains("semana", ignoreCase = true) -> ChartsPage.ChartType.TOP
              else -> ChartsPage.ChartType.TRENDING
            }
            fallbackSections.add(
              ChartsPage.ChartSection(
                title = section.title.ifEmpty { "Tendencias ahora" },
                items = songs,
                chartType = chartType
              )
            )
          }
        }

        if (fallbackSections.isNotEmpty()) {
          _chartsPage.value = ChartsPage(sections = fallbackSections, continuation = null)
        } else {
          // Fallback 2: Search trending songs
          val searchResult = YouTube.search("trending music", YouTube.SearchFilter.FILTER_SONG).getOrNull()
          val songs = searchResult?.items?.filterIsInstance<SongItem>().orEmpty()
          if (songs.isNotEmpty()) {
            _chartsPage.value = ChartsPage(
              sections = listOf(
                ChartsPage.ChartSection(
                  title = "Tendencias ahora",
                  items = songs,
                  chartType = ChartsPage.ChartType.TRENDING
                )
              ),
              continuation = null
            )
          } else {
            _error.value = "No se pudieron cargar las canciones"
          }
        }
      }

      _isLoading.value = false
    }
  }

  fun loadMore() {
    viewModelScope.launch {
      _chartsPage.value?.continuation?.let { continuation ->
        _isLoading.value = true
        YouTube.getChartsPage(continuation)
          .onSuccess { newPage ->
            _chartsPage.value =
              _chartsPage.value?.copy(
                sections = _chartsPage.value?.sections.orEmpty() + newPage.sections,
                continuation = newPage.continuation
              )
          }
          .onFailure { e -> _error.value = "Failed to load more: ${e.message}" }
        _isLoading.value = false
      }
    }
  }
}
