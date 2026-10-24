package echo.music.iad1tya.utils

import android.os.SystemClock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

object LoadTimeLogger {
    private const val TAG = "LOAD_TIME"

    private val starts = ConcurrentHashMap<String, Long>()

    fun start(
        key: String,
        stage: String,
        mediaId: String? = null,
        extra: String? = null,
    ) {
        val now = SystemClock.elapsedRealtime()
        starts[key] = now

        Timber
            .tag(TAG)
            .d(
                buildString {
                    append("START stage=")
                    append(stage)
                    append(" key=")
                    append(key)
                    if (!mediaId.isNullOrBlank()) {
                        append(" mediaId=")
                        append(mediaId)
                    }
                    if (!extra.isNullOrBlank()) {
                        append(" ")
                        append(extra)
                    }
                },
            )
    }

    fun mark(
        key: String,
        stage: String,
        mediaId: String? = null,
        extra: String? = null,
    ) {
        val startedAt = starts[key]
        val elapsed = startedAt?.let { SystemClock.elapsedRealtime() - it }

        Timber
            .tag(TAG)
            .d(
                buildString {
                    append("MARK stage=")
                    append(stage)
                    append(" key=")
                    append(key)
                    if (elapsed != null) {
                        append(" elapsed=")
                        append(elapsed)
                        append("ms")
                    }
                    if (!mediaId.isNullOrBlank()) {
                        append(" mediaId=")
                        append(mediaId)
                    }
                    if (!extra.isNullOrBlank()) {
                        append(" ")
                        append(extra)
                    }
                },
            )
    }

    fun end(
        key: String,
        stage: String,
        mediaId: String? = null,
        extra: String? = null,
    ) {
        val startedAt = starts.remove(key)
        val elapsed = startedAt?.let { SystemClock.elapsedRealtime() - it }

        Timber
            .tag(TAG)
            .d(
                buildString {
                    append("END stage=")
                    append(stage)
                    append(" key=")
                    append(key)
                    if (elapsed != null) {
                        append(" elapsed=")
                        append(elapsed)
                        append("ms")
                    }
                    if (!mediaId.isNullOrBlank()) {
                        append(" mediaId=")
                        append(mediaId)
                    }
                    if (!extra.isNullOrBlank()) {
                        append(" ")
                        append(extra)
                    }
                },
            )
    }

    fun fail(
        key: String,
        stage: String,
        mediaId: String? = null,
        throwable: Throwable,
    ) {
        val startedAt = starts.remove(key)
        val elapsed = startedAt?.let { SystemClock.elapsedRealtime() - it }

        Timber
            .tag(TAG)
            .e(
                throwable,
                buildString {
                    append("FAIL stage=")
                    append(stage)
                    append(" key=")
                    append(key)
                    if (elapsed != null) {
                        append(" elapsed=")
                        append(elapsed)
                        append("ms")
                    }
                    if (!mediaId.isNullOrBlank()) {
                        append(" mediaId=")
                        append(mediaId)
                    }
                },
            )
    }
}
