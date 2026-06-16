package iad1tya.echo.music.playback

import android.content.Context
import android.net.ConnectivityManager
import android.os.PowerManager
import iad1tya.echo.music.constants.AudioQualityBatteryProfileKey
import iad1tya.echo.music.constants.AudioQualityMobileProfileKey
import iad1tya.echo.music.constants.AudioQualityWifiProfileKey
import iad1tya.echo.music.constants.PlaybackAudioQualityProfile
import iad1tya.echo.music.utils.dataStore
import kotlinx.coroutines.flow.first

object PlaybackQualityResolver {
    suspend fun resolve(
        context: Context?,
        connectivityManager: ConnectivityManager,
    ): PlaybackAudioQualityProfile {
        if (context == null) {
            return if (connectivityManager.isActiveNetworkMetered) {
                PlaybackAudioQualityProfile.MEDIUM
            } else {
                PlaybackAudioQualityProfile.HIGH
            }
        }

        val preferences = context.dataStore.data.first()

        val wifiProfile =
            preferences[AudioQualityWifiProfileKey]
                .toPlaybackQualityProfile(PlaybackAudioQualityProfile.HIGH)

        val mobileProfile =
            preferences[AudioQualityMobileProfileKey]
                .toPlaybackQualityProfile(PlaybackAudioQualityProfile.MEDIUM)

        val batteryProfile =
            preferences[AudioQualityBatteryProfileKey]
                .toPlaybackQualityProfile(PlaybackAudioQualityProfile.LOW)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatterySaverEnabled = powerManager?.isPowerSaveMode == true

        return when {
            isBatterySaverEnabled -> batteryProfile.resolveAuto(PlaybackAudioQualityProfile.LOW)
            connectivityManager.isActiveNetworkMetered -> mobileProfile.resolveAuto(PlaybackAudioQualityProfile.MEDIUM)
            else -> wifiProfile.resolveAuto(PlaybackAudioQualityProfile.HIGH)
        }
    }

    private fun String?.toPlaybackQualityProfile(defaultValue: PlaybackAudioQualityProfile): PlaybackAudioQualityProfile =
        PlaybackAudioQualityProfile.entries.firstOrNull { it.name == this } ?: defaultValue

    private fun PlaybackAudioQualityProfile.resolveAuto(defaultValue: PlaybackAudioQualityProfile): PlaybackAudioQualityProfile =
        if (this == PlaybackAudioQualityProfile.AUTO) defaultValue else this
}
