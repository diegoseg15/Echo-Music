package echo.music.iad1tya.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.PlaybackMediaMode

@Composable
internal fun VideoModeButton(
    playbackMediaMode: PlaybackMediaMode,
    videoPlaybackAvailable: Boolean,
    videoPlaybackLoading: Boolean,
    videoPlaybackAvailabilityLoading: Boolean,
    isLocalMedia: Boolean,
    isCasting: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    onSwitchPlaybackMediaMode: (PlaybackMediaMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVideoPlaybackMode = playbackMediaMode == PlaybackMediaMode.VIDEO

    val nextPlaybackMediaMode =
        if (isVideoPlaybackMode) {
            PlaybackMediaMode.AUDIO
        } else {
            PlaybackMediaMode.VIDEO
        }

    val showVideoModeButton =
        !isLocalMedia &&
            !isCasting &&
            (videoPlaybackAvailable || isVideoPlaybackMode)

    AnimatedVisibility(
        visible = showVideoModeButton,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        FilledIconButton(
            onClick = {
                onSwitchPlaybackMediaMode(nextPlaybackMediaMode)
            },
            enabled = !videoPlaybackLoading && !videoPlaybackAvailabilityLoading,
            shape = RoundedCornerShape(24.dp),
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor =
                        if (isVideoPlaybackMode) {
                            textButtonColor
                        } else {
                            textButtonColor.copy(alpha = 0.2f)
                        },
                    contentColor =
                        if (isVideoPlaybackMode) {
                            iconButtonColor
                        } else {
                            textButtonColor
                        },
                ),
            modifier = Modifier.size(40.dp),
        ) {
            if (videoPlaybackLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painter =
                        painterResource(
                            if (isVideoPlaybackMode) {
                                R.drawable.ic_music_note
                            } else {
                                R.drawable.ic_video_mode
                            },
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
