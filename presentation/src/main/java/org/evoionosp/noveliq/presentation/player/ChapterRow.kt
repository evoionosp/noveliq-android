package org.evoionosp.noveliq.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.domain.audiobook.model.AudiobookChapter
import org.evoionosp.noveliq.presentation.R
import org.evoionosp.noveliq.presentation.icons.PlayingEqualizerIcon

@Composable
fun ChapterRow(
    chapter: AudiobookChapter,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
) {
    // Player roles: identical to the app theme in Book Details (via the
    // app-level fallback provider), cover-tinted inside the player sheets.
    val pc = LocalPlayerColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color =
                    if (isCurrent) {
                        pc.accent
                    } else {
                        pc.textPrimary
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = chapter.startInSeconds.toDurationLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = pc.textSecondary,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Trailing control: the live equalizer for the currently-playing chapter (non-clickable),
        // otherwise a play button. Kept on the same side so titles get the full remaining width.
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                PlayingEqualizerIcon(isAnimating = isPlaying, color = pc.accent)
            } else {
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.now_playing_play),
                        tint = pc.accent,
                    )
                }
            }
        }
    }
}

/**
 * Index of the chapter containing [positionSeconds], or -1 when unknown. Works for
 * saved (details) and live (playing) positions alike — play state only animates the
 * marker, it never changes which row is current.
 */
internal fun inProgressChapterIndex(
    chapters: List<AudiobookChapter>,
    positionSeconds: Double,
): Int = chapters.indexOfLast { it.startInSeconds <= positionSeconds }
