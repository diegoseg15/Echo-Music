package echo.music.iad1tya.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.ChartsPage
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.extensions.togglePlayPause
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.menu.SongMenu
import echo.music.iad1tya.ui.menu.YouTubeSongMenu
import echo.music.iad1tya.utils.makeTimeString
import echo.music.iad1tya.viewmodels.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHomeScreen(
  navController: NavController,
  snackbarHostState: SnackbarHostState,
  viewModel: HomeViewModel,
  modifier: Modifier = Modifier,
) {
  val menuState = LocalMenuState.current
  val database = LocalDatabase.current
  val playerConnection = LocalPlayerConnection.current ?: return
  val haptic = LocalHapticFeedback.current

  val isEffectivelyPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
  val duration by playerConnection.duration.collectAsState()

  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val pullRefreshState = rememberPullToRefreshState()
  val listState = rememberLazyListState()

  val recentlyPlayedDbSongs by viewModel.recentlyPlayedSongs.collectAsState()
  val quickPicks by viewModel.quickPicks.collectAsState()
  val heroSongs by viewModel.heroSongs.collectAsState()
  val homePage by viewModel.homePage.collectAsState()
  val chartsPage by viewModel.chartsPage.collectAsState()

  // Track real-time playback position for the featured card
  var currentPositionMs by remember { mutableLongStateOf(0L) }
  LaunchedEffect(isEffectivelyPlaying, mediaMetadata) {
    while (isActive) {
      currentPositionMs = try {
        playerConnection.player.currentPosition.coerceAtLeast(0L)
      } catch (_: Exception) {
        0L
      }
      delay(500L)
    }
  }

  // 1. Prepare "Recently Played" songs
  val recentlyPlayedSongs: List<SongItem> = remember(recentlyPlayedDbSongs, quickPicks, heroSongs) {
    if (recentlyPlayedDbSongs.isNotEmpty()) {
      recentlyPlayedDbSongs.map { song ->
        SongItem(
          id = song.id,
          title = song.title,
          artists = song.artists.map { Artist(name = it.name, id = it.id) },
          album = null,
          duration = song.song.duration,
          thumbnail = song.thumbnailUrl ?: "",
          explicit = false,
        )
      }
    } else if (!quickPicks.isNullOrEmpty()) {
      quickPicks.orEmpty().map { song ->
        SongItem(
          id = song.id,
          title = song.title,
          artists = song.artists.map { Artist(name = it.name, id = it.id) },
          album = null,
          duration = song.song.duration,
          thumbnail = song.thumbnailUrl ?: "",
          explicit = false,
        )
      }
    } else {
      heroSongs
    }
  }

  // 2. Prepare "Trending Now" songs
  val trendingSongs: List<SongItem> = remember(chartsPage, homePage, heroSongs) {
    val fromCharts = chartsPage?.sections
      ?.find { it.chartType == ChartsPage.ChartType.TRENDING }
      ?.items
      ?.filterIsInstance<SongItem>()
      .orEmpty()

    if (fromCharts.isNotEmpty()) {
      fromCharts
    } else {
      val fromHome = homePage?.sections
        ?.flatMap { it.items }
        ?.filterIsInstance<SongItem>()
        .orEmpty()

      if (fromHome.isNotEmpty()) fromHome else heroSongs
    }
  }

  // 3. Prepare "Songs of the Week"
  val songsOfTheWeek: List<SongItem> = remember(chartsPage, homePage, trendingSongs, quickPicks) {
    val fromTopCharts = chartsPage?.sections
      ?.find { it.chartType == ChartsPage.ChartType.TOP }
      ?.items
      ?.filterIsInstance<SongItem>()
      .orEmpty()

    if (fromTopCharts.isNotEmpty()) {
      fromTopCharts
    } else {
      val candidateSections = homePage?.sections.orEmpty()
      val secondSection = candidateSections.getOrNull(1)?.items?.filterIsInstance<SongItem>().orEmpty()
      if (secondSection.isNotEmpty()) {
        secondSection
      } else if (trendingSongs.size > 5) {
        trendingSongs.drop(4)
      } else {
        quickPicks.orEmpty().map { song ->
          SongItem(
            id = song.id,
            title = song.title,
            artists = song.artists.map { Artist(name = it.name, id = it.id) },
            album = null,
            duration = song.song.duration,
            thumbnail = song.thumbnailUrl ?: "",
            explicit = false,
          )
        }
      }
    }
  }

  PullToRefreshBox(
    state = pullRefreshState,
    isRefreshing = isRefreshing,
    onRefresh = viewModel::refresh,
    indicator = {
      PullToRefreshDefaults.LoadingIndicator(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
      )
    },
    modifier = modifier.fillMaxSize(),
  ) {
    LazyColumn(
      state = listState,
      contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
      verticalArrangement = Arrangement.spacedBy(22.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item(key = "top_spacing") {
        Spacer(modifier = Modifier.height(4.dp))
      }

      // SECTION 1: RECENTLY PLAYED
      item(key = "section_recently_played") {
        RecentlyPlayedSection(
          songs = recentlyPlayedSongs.take(4),
          onSeeAllClick = { navController.navigate("history") },
          onSongClick = { song ->
            playerConnection.playQueue(
              YouTubeQueue(
                song.endpoint ?: WatchEndpoint(videoId = song.id),
                song.toMediaMetadata(),
              )
            )
          },
          onSongLongClick = { song ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
              YouTubeSongMenu(
                song = song,
                navController = navController,
                onDismiss = menuState::dismiss,
              )
            }
          },
        )
      }

      // SECTION 2: TRENDING NOW
      item(key = "section_trending_now") {
        TrendingNowSection(
          songs = trendingSongs.take(12),
          onSeeAllClick = { navController.navigate("charts") },
          onSongClick = { song ->
            playerConnection.playQueue(
              YouTubeQueue(
                song.endpoint ?: WatchEndpoint(videoId = song.id),
                song.toMediaMetadata(),
              )
            )
          },
          onSongLongClick = { song ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
              YouTubeSongMenu(
                song = song,
                navController = navController,
                onDismiss = menuState::dismiss,
              )
            }
          },
        )
      }

      // SECTION 3: SONGS OF THE WEEK
      item(key = "section_songs_of_the_week_header") {
        HomeSectionHeader(
          title = "Songs of the week",
          onSeeAllClick = { navController.navigate("charts") },
        )
      }

      // Active / Featured Now Playing Card
      item(key = "featured_playback_card") {
        FeaturedPlaybackCard(
          activeMediaMetadata = mediaMetadata,
          isPlaying = isEffectivelyPlaying,
          positionMs = currentPositionMs,
          durationMs = duration,
          fallbackSong = songsOfTheWeek.firstOrNull(),
          onPlayPauseClick = { playerConnection.togglePlayPause() },
          onFallbackPlayClick = { song ->
            playerConnection.playQueue(
              YouTubeQueue(
                song.endpoint ?: WatchEndpoint(videoId = song.id),
                song.toMediaMetadata(),
              )
            )
          },
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }

      // Remaining Ranked Song Cards
      val rankedSongs = songsOfTheWeek.drop(1).take(10)
      itemsIndexed(
        items = rankedSongs,
        key = { index, song -> "ranked_${song.id}_$index" },
      ) { index, song ->
        val percentage = 20 - (index * 2).coerceAtLeast(0) + 1
        val badgeText = "+$percentage%"

        RankedSongRow(
          song = song,
          badgeText = badgeText,
          onClick = {
            playerConnection.playQueue(
              YouTubeQueue(
                song.endpoint ?: WatchEndpoint(videoId = song.id),
                song.toMediaMetadata(),
              )
            )
          },
          onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
              YouTubeSongMenu(
                song = song,
                navController = navController,
                onDismiss = menuState::dismiss,
              )
            }
          },
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }

      item(key = "bottom_spacing") {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

/**
 * Clean Section Header with title and subtle "See all ->" action
 */
@Composable
fun HomeSectionHeader(
  title: String,
  onSeeAllClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp,
      ),
      color = MaterialTheme.colorScheme.onBackground,
    )

    if (onSeeAllClick != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .combinedClickable(onClick = onSeeAllClick)
          .padding(horizontal = 4.dp, vertical = 4.dp),
      ) {
        Text(
          text = "See all",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
        Icon(
          painter = painterResource(R.drawable.arrow_forward),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          modifier = Modifier.size(13.dp),
        )
      }
    }
  }
}

/**
 * Section 1: Recently Played 2x2 Grid of pill-shaped cards
 */
@Composable
fun RecentlyPlayedSection(
  songs: List<SongItem>,
  onSeeAllClick: () -> Unit,
  onSongClick: (SongItem) -> Unit,
  onSongLongClick: (SongItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    HomeSectionHeader(
      title = "Recently played",
      onSeeAllClick = onSeeAllClick,
    )

    val displaySongs = songs.take(4)
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
      for (rowIndex in 0 until 2) {
        val startIndex = rowIndex * 2
        val rowSongs = displaySongs.drop(startIndex).take(2)
        if (rowSongs.isNotEmpty()) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            for (song in rowSongs) {
              RecentlyPlayedCard(
                song = song,
                onClick = { onSongClick(song) },
                onLongClick = { onSongLongClick(song) },
                modifier = Modifier.weight(1f),
              )
            }
            if (rowSongs.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
fun RecentlyPlayedCard(
  song: SongItem,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val cardBg = if (isDark) {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  } else {
    Color.White
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
    ) {
      AsyncImage(
        model = song.thumbnail,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.weight(1f),
      ) {
        val artistText = song.artists.joinToString(", ") { it.name }.ifEmpty { "Artist" }
        Text(
          text = artistText,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = song.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Section 2: Trending Now with large rounded artwork and centered translucent play button
 */
@Composable
fun TrendingNowSection(
  songs: List<SongItem>,
  onSeeAllClick: () -> Unit,
  onSongClick: (SongItem) -> Unit,
  onSongLongClick: (SongItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    HomeSectionHeader(
      title = "Trending now",
      onSeeAllClick = onSeeAllClick,
    )

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(songs, key = { it.id }) { song ->
        TrendingSongCard(
          song = song,
          onClick = { onSongClick(song) },
          onLongClick = { onSongLongClick(song) },
        )
      }
    }
  }
}

@Composable
fun TrendingSongCard(
  song: SongItem,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()

  Column(
    modifier = modifier
      .width(140.dp)
      .clip(RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(140.dp)
        .shadow(
          elevation = if (isDark) 4.dp else 8.dp,
          shape = RoundedCornerShape(20.dp),
          spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        )
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
      AsyncImage(
        model = song.thumbnail,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )

      // Translucent Circular Play Button in the center
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.88f))
          .shadow(elevation = 2.dp, shape = CircleShape),
      ) {
        Icon(
          painter = painterResource(R.drawable.play),
          contentDescription = null,
          tint = Color(0xFF1E1E24),
          modifier = Modifier
            .size(18.dp)
            .padding(start = 2.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = song.title,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
      ),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    val artistText = song.artists.joinToString(", ") { it.name }.ifEmpty { "Artist" }
    Text(
      text = artistText,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Section 3: Featured Active Playback Card (Item 1 of Songs of the week)
 */
@Composable
fun FeaturedPlaybackCard(
  activeMediaMetadata: echo.music.iad1tya.models.MediaMetadata?,
  isPlaying: Boolean,
  positionMs: Long,
  durationMs: Long,
  fallbackSong: SongItem?,
  onPlayPauseClick: () -> Unit,
  onFallbackPlayClick: (SongItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val cardBg = if (isDark) {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  } else {
    Color.White
  }

  val hasActivePlayback = activeMediaMetadata != null
  val title = if (hasActivePlayback) activeMediaMetadata.title else fallbackSong?.title ?: "Track Title"
  val artist = if (hasActivePlayback) {
    activeMediaMetadata.artists.joinToString(", ") { it.name }.ifEmpty { "Artist" }
  } else {
    fallbackSong?.artists?.joinToString(", ") { it.name } ?: "Artist"
  }
  val thumbnail = if (hasActivePlayback) activeMediaMetadata.thumbnailUrl else fallbackSong?.thumbnail

  val effectiveDurationMs = if (hasActivePlayback && durationMs > 0) {
    durationMs
  } else {
    (fallbackSong?.duration ?: 210) * 1000L
  }
  val effectivePositionMs = if (hasActivePlayback) positionMs else 0L

  val progressFraction = if (effectiveDurationMs > 0) {
    (effectivePositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 3.dp),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = {
          if (!hasActivePlayback && fallbackSong != null) {
            onFallbackPlayClick(fallbackSong)
          } else {
            onPlayPauseClick()
          }
        },
      ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        AsyncImage(
          model = thumbnail,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.5.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        IconButton(
          onClick = {
            if (!hasActivePlayback && fallbackSong != null) {
              onFallbackPlayClick(fallbackSong)
            } else {
              onPlayPauseClick()
            }
          },
          colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface,
          ),
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape),
        ) {
          Icon(
            painter = painterResource(
              if (hasActivePlayback && isPlaying) R.drawable.pause else R.drawable.play
            ),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Scrubber progress bar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = makeTimeString(effectivePositionMs),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )

        LinearProgressIndicator(
          progress = { progressFraction },
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier
            .weight(1f)
            .height(4.dp)
            .clip(CircleShape),
        )

        Text(
          text = makeTimeString(effectiveDurationMs),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      }
    }
  }
}

/**
 * Section 3: Ranked Song Row with trend percentage pill badge
 */
@Composable
fun RankedSongRow(
  song: SongItem,
  badgeText: String,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = isSystemInDarkTheme()
  val cardBg = if (isDark) {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  } else {
    Color.White
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
      AsyncImage(
        model = song.thumbnail,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        val artistText = song.artists.joinToString(", ") { it.name }.ifEmpty { "Artist" }
        Text(
          text = artistText,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = song.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Trend percentage pill badge
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
          .padding(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Text(
          text = badgeText,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
