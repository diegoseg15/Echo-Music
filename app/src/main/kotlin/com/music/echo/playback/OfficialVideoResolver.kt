package echo.music.iad1tya.playback

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

internal class OfficialVideoResolver {
    private val officialVideoIdCache = mutableMapOf<String, String?>()

    fun cacheLinkedVideoId(
        songId: String,
        videoId: String,
    ) {
        officialVideoIdCache[songId] = videoId
    }

    suspend fun resolveOfficialVideoIdForSong(mediaMetadata: MediaMetadata): String? =
        withContext(Dispatchers.IO) {
            if (officialVideoIdCache.containsKey(mediaMetadata.id)) {
                return@withContext officialVideoIdCache[mediaMetadata.id]
            }

            val title = mediaMetadata.title.trim()
            val artists =
                mediaMetadata
                    .artists
                    .joinToString(" ") { it.name }
                    .trim()

            if (title.isBlank()) {
                return@withContext null
            }

            val queries =
                listOf(
                    "$artists $title official music video",
                    "$artists $title",
                    "$title $artists official video",
                ).map { it.trim() }
                    .distinct()
                    .filter { it.isNotBlank() }

            Timber
                .tag(TAG)
                .d(
                    "VIDEO_MODE_SEARCH: resolving official video. " +
                        "songId=${mediaMetadata.id}, title=$title, artists=$artists",
                )

            queries.forEach { query ->
                val result =
                    YouTube
                        .search(
                            query = query,
                            filter = YouTube.SearchFilter.FILTER_VIDEO,
                        ).getOrNull()

                val candidates =
                    result
                        ?.items
                        .orEmpty()
                        .filterIsInstance<SongItem>()
                        .map { it.toMediaMetadata() }
                        .filter { candidate ->
                            candidate.id != mediaMetadata.id && candidate.isVideoSong
                        }.map { candidate ->
                            candidate to
                                scoreVideoCandidate(
                                    source = mediaMetadata,
                                    candidate = candidate,
                                )
                        }.sortedByDescending { it.second }

                candidates
                    .take(5)
                    .forEach { (candidate, score) ->
                        Timber
                            .tag(TAG)
                            .d(
                                "VIDEO_MODE_SEARCH_CANDIDATE: " +
                                    "query=$query, " +
                                    "id=${candidate.id}, " +
                                    "title=${candidate.title}, " +
                                    "artists=${candidate.artists.joinToString { it.name }}, " +
                                    "musicVideoType=${candidate.musicVideoType}, " +
                                    "score=$score",
                            )
                    }

                val best =
                    candidates
                        .firstOrNull { (_, score) -> score >= 90 }
                        ?.first

                if (best != null) {
                    Timber
                        .tag(TAG)
                        .d(
                            "VIDEO_MODE_SEARCH_MATCH: " +
                                "songId=${mediaMetadata.id}, " +
                                "videoId=${best.id}, " +
                                "title=${best.title}",
                        )

                    officialVideoIdCache[mediaMetadata.id] = best.id
                    return@withContext best.id
                }
            }

            Timber
                .tag(TAG)
                .w(
                    "VIDEO_MODE_SEARCH: no official video found. " +
                        "songId=${mediaMetadata.id}, title=$title, artists=$artists",
                )

            officialVideoIdCache[mediaMetadata.id] = null

            null
        }

    private fun scoreVideoCandidate(
        source: MediaMetadata,
        candidate: MediaMetadata,
    ): Int {
        val sourceTitle = normalizeVideoMatchText(source.title)
        val candidateTitle = normalizeVideoMatchText(candidate.title)

        val sourceArtists =
            source
                .artists
                .map { normalizeVideoMatchText(it.name) }
                .filter { it.isNotBlank() }

        val candidateArtists =
            candidate
                .artists
                .map { normalizeVideoMatchText(it.name) }
                .filter { it.isNotBlank() }

        var score = 0

        if (candidate.isVideoSong) {
            score += 100
        }

        if (candidate.musicVideoType == "MUSIC_VIDEO_TYPE_OMV") {
            score += 40
        }

        if (candidateTitle == sourceTitle) {
            score += 80
        } else if (candidateTitle.contains(sourceTitle) || sourceTitle.contains(candidateTitle)) {
            score += 45
        }

        if (
            sourceArtists.isNotEmpty() &&
            sourceArtists.any { sourceArtist ->
                candidateArtists.any { candidateArtist ->
                    candidateArtist.contains(sourceArtist) || sourceArtist.contains(candidateArtist)
                }
            }
        ) {
            score += 45
        }

        val badTokens =
            listOf(
                "lyrics",
                "lyric",
                "karaoke",
                "cover",
                "reaction",
                "remix",
                "sped up",
                "slowed",
                "nightcore",
                "visualizer",
                "audio",
            )

        if (badTokens.any { candidateTitle.contains(it) }) {
            score -= 70
        }

        return score
    }

    private fun normalizeVideoMatchText(value: String): String =
        value
            .lowercase(Locale.ROOT)
            .replace("[^\\p{L}\\p{Nd}]+".toRegex(), " ")
            .trim()

    companion object {
        private const val TAG = "OfficialVideoResolver"
    }
}