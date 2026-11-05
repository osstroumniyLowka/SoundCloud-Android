package com.scd.android.ui.wavefeed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.scd.android.data.Library
import com.scd.android.data.NowPlaying
import com.scd.android.data.Playback
import com.scd.android.data.Track

@Composable
fun LiteWaveFeed(modifier: Modifier = Modifier) {
    val tracks by Library.tracksFlow.collectAsState()
    val currentTrack by NowPlaying.currentTrackFlow.collectAsState()
    val isPlaying by Playback.isPlayingFlow.collectAsState()

    // Маппинг в UI-модель делаем один раз на каждое изменение списка
    val uiTracks = remember(tracks) { tracks.map { it.toWaveFeedTrack() } }

    // Ищем исходный Track по id, чтобы передать его в плеер
    val tracksById = remember(tracks) { tracks.associateBy { it.id } }

    WaveFeedScreen(
        tracks = uiTracks,
        currentTrackId = currentTrack?.id,
        isPlaying = isPlaying,
        onTrackClick = { ui ->
            tracksById[ui.id]?.let { Playback.play(it) }
        },
        modifier = modifier,
    )
}

private fun Track.toWaveFeedTrack() = WaveFeedTrack(
    id = id,
    title = title,
    artist = artist,
)