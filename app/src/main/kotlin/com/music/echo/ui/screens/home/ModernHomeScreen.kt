package echo.music.iad1tya.ui.screens.home

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.Artist as InnertubeArtist
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.ChartsPage
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ArtistSortType
import echo.music.iad1tya.constants.PlaybackMediaMode
import echo.music.iad1tya.constants.SongSortType
import echo.music.iad1tya.db.entities.Artist
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.menu.ArtistMenu
import echo.music.iad1tya.ui.menu.YouTubePlaylistMenu
import echo.music.iad1tya.ui.menu.YouTubeSongMenu
import echo.music.iad1tya.utils.makeTimeString
import echo.music.iad1tya.viewmodels.HomeViewModel

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
  val scope = rememberCoroutineScope()

  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val pullRefreshState = rememberPullToRefreshState()
  val listState = rememberLazyListState()

  val recentlyPlayedDbSongs by viewModel.recentlyPlayedSongs.collectAsState()
  val quickPicks by viewModel.quickPicks.collectAsState()
  val heroSongs by viewModel.heroSongs.collectAsState()
  val homePage by viewModel.homePage.collectAsState()
  val chartsPage by viewModel.chartsPage.collectAsState()
  val communityPlaylists by viewModel.communityPlaylists.collectAsState()

  // Favorite Artists from local database
  val bookmarkedArtists by database.artistsBookmarked(ArtistSortType.PLAY_TIME, descending = true)
    .collectAsState(initial = emptyList())
  val playedArtists by database.artists(ArtistSortType.PLAY_TIME, descending = true)
    .collectAsState(initial = emptyList())
  val favoriteArtists = remember(bookmarkedArtists, playedArtists) {
    if (bookmarkedArtists.isNotEmpty()) bookmarkedArtists else playedArtists.take(10)
  }

  // Favorite songs for video section
  val likedSongs by database.likedSongs(SongSortType.PLAY_TIME, descending = true)
    .collectAsState(initial = emptyList())

  // 1. Prepare "Escuchado recientemente"
  val recentlyPlayedSongs: List<SongItem> = remember(recentlyPlayedDbSongs, quickPicks, heroSongs) {
    if (recentlyPlayedDbSongs.isNotEmpty()) {
      recentlyPlayedDbSongs.map { song ->
        SongItem(
          id = song.id,
          title = song.title,
          artists = song.artists.map { InnertubeArtist(name = it.name, id = it.id) },
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
          artists = song.artists.map { InnertubeArtist(name = it.name, id = it.id) },
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

  // 2. Prepare "Tendencias ahora"
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

  // 3. Prepare "Canciones de la semana"
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
            artists = song.artists.map { InnertubeArtist(name = it.name, id = it.id) },
            album = null,
            duration = song.song.duration,
            thumbnail = song.thumbnailUrl ?: "",
            explicit = false,
          )
        }
      }
    }
  }

  // 4. Prepare "Playlists recomendadas"
  val recommendedPlaylists: List<PlaylistItem> = remember(communityPlaylists, homePage) {
    val fromCommunity = communityPlaylists.orEmpty().map { it.playlist }
    if (fromCommunity.isNotEmpty()) {
      fromCommunity
    } else {
      homePage?.sections.orEmpty().flatMap { it.items }.filterIsInstance<PlaylistItem>()
    }
  }

  // 5. Prepare "Videos de tus canciones favoritas"
  val favoriteVideoSongs: List<SongItem> = remember(likedSongs, recentlyPlayedSongs, trendingSongs) {
    if (likedSongs.isNotEmpty()) {
      likedSongs.take(8).map { song ->
        SongItem(
          id = song.id,
          title = song.title,
          artists = song.artists.map { InnertubeArtist(name = it.name, id = it.id) },
          album = null,
          duration = song.song.duration,
          thumbnail = song.thumbnailUrl ?: "",
          explicit = false,
        )
      }
    } else if (recentlyPlayedSongs.isNotEmpty()) {
      recentlyPlayedSongs.take(8)
    } else {
      trendingSongs.take(8)
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

      // SECCIÓN 1: ESCUCHADO RECIENTEMENTE (Llenado dinámico sin huecos)
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

      // SECCIÓN 2: TENDENCIAS AHORA
      item(key = "section_trending_now") {
        TrendingNowSection(
          songs = trendingSongs.take(12),
          onSeeAllClick = { navController.navigate("charts_screen?section=trending") },
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

      // SECCIÓN 3: CANCIONES DE LA SEMANA (Solo lista clasificada)
      item(key = "section_songs_of_the_week_header") {
        HomeSectionHeader(
          title = "Canciones de la semana",
          onSeeAllClick = { navController.navigate("charts_screen?section=top") },
        )
      }

      itemsIndexed(
        items = songsOfTheWeek.take(8),
        key = { index, song -> "songs_of_week_${song.id}_$index" },
      ) { index, song ->
        val percentage = (24 - index * 2).coerceAtLeast(8)
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

      // SECCIÓN 4: PLAYLISTS RECOMENDADAS
      if (recommendedPlaylists.isNotEmpty()) {
        item(key = "section_recommended_playlists") {
          RecommendedPlaylistsSection(
            playlists = recommendedPlaylists.take(10),
            onPlaylistClick = { playlist ->
              val playlistId = playlist.id.removePrefix("VL")
              when (playlistId) {
                "LM" -> navController.navigate("auto_playlist/liked")
                "SE" -> navController.navigate("auto_playlist/downloaded")
                else -> navController.navigate("online_playlist/$playlistId")
              }
            },
            onPlaylistLongClick = { playlist ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              menuState.show {
                YouTubePlaylistMenu(
                  playlist = playlist,
                  coroutineScope = scope,
                  onDismiss = menuState::dismiss,
                )
              }
            },
          )
        }
      }

      // SECCIÓN 5: ARTISTAS FAVORITOS (Si es que hay)
      if (favoriteArtists.isNotEmpty()) {
        item(key = "section_favorite_artists") {
          FavoriteArtistsSection(
            artists = favoriteArtists.take(12),
            onArtistClick = { artist ->
              navController.navigate("artist/${artist.id}")
            },
            onArtistLongClick = { artist ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              menuState.show {
                ArtistMenu(
                  originalArtist = artist,
                  coroutineScope = scope,
                  onDismiss = menuState::dismiss,
                )
              }
            },
          )
        }
      }

      // SECCIÓN 6: VIDEOS DE TUS CANCIONES FAVORITAS (Como en la imagen de referencia)
      if (favoriteVideoSongs.isNotEmpty()) {
        item(key = "section_favorite_videos") {
          FavoriteVideosSection(
            songs = favoriteVideoSongs,
            onPlayVideoClick = { song ->
              playerConnection.playQueue(
                YouTubeQueue(
                  song.endpoint ?: WatchEndpoint(videoId = song.id),
                  song.toMediaMetadata(),
                )
              )
              playerConnection.service.switchPlaybackMediaMode(PlaybackMediaMode.VIDEO)
            },
            onMenuClick = { song ->
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
      }

      item(key = "bottom_spacing") {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

/**
 * Encabezado de sección limpio con título en español y acción "Ver todo ->"
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
          .padding(horizontal = 6.dp, vertical = 4.dp),
      ) {
        Text(
          text = "Ver todo",
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
 * Sección 1: Escuchado recientemente con llenado dinámico que nunca deja huecos vacíos
 */
@Composable
fun RecentlyPlayedSection(
  songs: List<SongItem>,
  onSeeAllClick: () -> Unit,
  onSongClick: (SongItem) -> Unit,
  onSongLongClick: (SongItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (songs.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    HomeSectionHeader(
      title = "Escuchado recientemente",
      onSeeAllClick = onSeeAllClick,
    )

    val count = songs.size.coerceAtMost(4)
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
      when (count) {
        1 -> {
          // Si solo hay 1 canción, llena todo el ancho a lo largo
          RecentlyPlayedCard(
            song = songs[0],
            onClick = { onSongClick(songs[0]) },
            onLongClick = { onSongLongClick(songs[0]) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        2 -> {
          // 2 canciones en 1 fila
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            RecentlyPlayedCard(
              song = songs[0],
              onClick = { onSongClick(songs[0]) },
              onLongClick = { onSongLongClick(songs[0]) },
              modifier = Modifier.weight(1f),
            )
            RecentlyPlayedCard(
              song = songs[1],
              onClick = { onSongClick(songs[1]) },
              onLongClick = { onSongLongClick(songs[1]) },
              modifier = Modifier.weight(1f),
            )
          }
        }
        3 -> {
          // Fila 1 con 2 canciones, Fila 2 con 1 canción ocupando todo el ancho
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            RecentlyPlayedCard(
              song = songs[0],
              onClick = { onSongClick(songs[0]) },
              onLongClick = { onSongLongClick(songs[0]) },
              modifier = Modifier.weight(1f),
            )
            RecentlyPlayedCard(
              song = songs[1],
              onClick = { onSongClick(songs[1]) },
              onLongClick = { onSongLongClick(songs[1]) },
              modifier = Modifier.weight(1f),
            )
          }
          RecentlyPlayedCard(
            song = songs[2],
            onClick = { onSongClick(songs[2]) },
            onLongClick = { onSongLongClick(songs[2]) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        else -> {
          // 4 canciones en 2 filas completas de 2 columnas
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            RecentlyPlayedCard(
              song = songs[0],
              onClick = { onSongClick(songs[0]) },
              onLongClick = { onSongLongClick(songs[0]) },
              modifier = Modifier.weight(1f),
            )
            RecentlyPlayedCard(
              song = songs[1],
              onClick = { onSongClick(songs[1]) },
              onLongClick = { onSongLongClick(songs[1]) },
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            RecentlyPlayedCard(
              song = songs[2],
              onClick = { onSongClick(songs[2]) },
              onLongClick = { onSongLongClick(songs[2]) },
              modifier = Modifier.weight(1f),
            )
            RecentlyPlayedCard(
              song = songs[3],
              onClick = { onSongClick(songs[3]) },
              onLongClick = { onSongLongClick(songs[3]) },
              modifier = Modifier.weight(1f),
            )
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
        val artistText = song.artists.joinToString(", ") { it.name }.ifEmpty { "Artista" }
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
 * Sección 2: Tendencias ahora (Sin recortes en los textos y con separación óptima)
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
      title = "Tendencias ahora",
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

      // Botón Play translúcido en el centro de la carátula
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

    Spacer(modifier = Modifier.height(3.dp))

    val artistText = song.artists.joinToString(", ") { it.name }.ifEmpty { "Artista" }
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
 * Sección 3: Fila de canción clasificada con badge de porcentaje
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
        val artistText = song.artists.joinToString(", ") { it.name }.ifEmpty { "Artista" }
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

/**
 * Sección 4: Playlists recomendadas
 */
@Composable
fun RecommendedPlaylistsSection(
  playlists: List<PlaylistItem>,
  onPlaylistClick: (PlaylistItem) -> Unit,
  onPlaylistLongClick: (PlaylistItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    HomeSectionHeader(title = "Playlists recomendadas")

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(playlists, key = { it.id }) { playlist ->
        Column(
          modifier = Modifier
            .width(140.dp)
            .combinedClickable(
              onClick = { onPlaylistClick(playlist) },
              onLongClick = { onPlaylistLongClick(playlist) },
            ),
        ) {
          AsyncImage(
            model = playlist.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(140.dp)
              .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
              .clip(RoundedCornerShape(18.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(2.dp))

          val authorText = playlist.author?.name ?: playlist.songCountText ?: "Playlist"
          Text(
            text = authorText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

/**
 * Sección 5: Artistas favoritos (Avatares circulares limpios)
 */
@Composable
fun FavoriteArtistsSection(
  artists: List<Artist>,
  onArtistClick: (Artist) -> Unit,
  onArtistLongClick: (Artist) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    HomeSectionHeader(title = "Artistas favoritos")

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(artists, key = { it.id }) { artist ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(88.dp)
            .combinedClickable(
              onClick = { onArtistClick(artist) },
              onLongClick = { onArtistLongClick(artist) },
            ),
        ) {
          AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(80.dp)
              .shadow(elevation = 4.dp, shape = CircleShape)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant),
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = artist.title,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

/**
 * Sección 6: Videos de tus canciones favoritas (Diseño exacto de la imagen de referencia)
 */
@Composable
fun FavoriteVideosSection(
  songs: List<SongItem>,
  onPlayVideoClick: (SongItem) -> Unit,
  onMenuClick: (SongItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    HomeSectionHeader(title = "Videos de tus canciones favoritas")

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(songs, key = { "fav_video_${it.id}" }) { song ->
        Column(
          modifier = Modifier
            .width(310.dp)
            .clip(RoundedCornerShape(16.dp)),
        ) {
          // Tarjeta de video 16:9 con botón "Reproducir el video completo"
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(16f / 9f)
              .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
              .clip(RoundedCornerShape(16.dp))
              .background(Color.Black)
              .combinedClickable(
                onClick = { onPlayVideoClick(song) },
                onLongClick = { onMenuClick(song) },
              ),
          ) {
            AsyncImage(
              model = song.thumbnail,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )

            // Píldora negra central "Reproducir el video completo"
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .combinedClickable(
                  onClick = { onPlayVideoClick(song) },
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
              Text(
                text = "Reproducir el video completo",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                ),
                color = Color.White,
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Fila inferior: Avatar de artista circular, título, artista y duración, y menú de 3 puntos
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp),
          ) {
            AsyncImage(
              model = song.thumbnail,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
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

              val artistName = song.artists.firstOrNull()?.name ?: "Artista"
              val songDuration = song.duration
              val durationText = if (songDuration != null && songDuration > 0) {
                "${songDuration / 60} min"
              } else {
                "Video oficial"
              }
              Text(
                text = "$artistName • $durationText",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            IconButton(
              onClick = { onMenuClick(song) },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }
}
