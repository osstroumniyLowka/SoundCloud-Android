package com.scd.android.ui.wavefeed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import com.scd.android.ui.theme.AmoledBlack
import com.scd.android.ui.theme.AmoledSurfaceVariant
import com.scd.android.ui.theme.IridescentColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------- Модель (замените на вашу модель трека) ----------

data class WaveFeedTrack(
    val id: Int,
    val title: String,
    val artist: String,
)

private const val TWO_PI = (2.0 * PI).toFloat()

// ---------- Экран ----------

/**
 * Лента WaveFeed. Плеер (PlayerBar) не входит в экран: он живёт в родительском Scaffold.
 * Данные передаются извне, чтобы экран не зависел от синглтонов NowPlaying/Playback.
 */
@Composable
fun WaveFeedScreen(
    tracks: List<WaveFeedTrack>,
    currentTrackId: Int?,
    isPlaying: Boolean,
    onTrackClick: (WaveFeedTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        item {
            Text(
                text = "Добро пожаловать",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            WaveBlob(
                active = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Недавно прослушано",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
        }

        items(tracks, key = { it.id }) { track ->
            WaveTrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                onClick = { onTrackClick(track) },
            )
        }
    }
}

// ---------- Фигура с переливом ----------

/**
 * Время анимации считается по кадрам и пишется в state, который читается
 * только внутри DrawScope. Поэтому перерисовывается только Canvas, без рекомпозиции экрана.
 * Когда воспроизведение остановлено, анимация замирает, и батарея не расходуется.
 */
@Composable
private fun WaveBlob(active: Boolean, modifier: Modifier = Modifier) {
    var elapsed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0L) elapsed += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }

    Canvas(modifier = modifier) {
        val phase = elapsed * TWO_PI / 12f            // полный цикл деформации: 12 с
        val spin = (elapsed * 360f / 18f) % 360f      // полный оборот градиента: 18 с

        val center = Offset(size.width / 2f, size.height / 2f)
        val base = min(size.width, size.height) * 0.34f

        // Свечение под фигурой (акцент темы на чёрном)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    IridescentColors.first().copy(alpha = 0.22f),
                    Color.Transparent,
                ),
                center = center,
                radius = base * 1.8f,
            ),
            radius = base * 1.8f,
            center = center,
        )

        // Основная фигура с переливом
        rotate(degrees = spin, pivot = center) {
            drawPath(
                path = blobPath(center, base, phase),
                brush = Brush.sweepGradient(IridescentColors, center),
            )
        }

        // Блик, который плавает по фигуре
        val highlightCenter = Offset(
            x = center.x + base * 0.25f * cos(phase),
            y = center.y - base * 0.3f + base * 0.15f * sin(phase),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                center = highlightCenter,
                radius = base * 0.9f,
            ),
            radius = base * 0.9f,
            center = highlightCenter,
        )
    }
}

private fun blobPath(center: Offset, baseRadius: Float, phase: Float): Path {
    val path = Path()
    val steps = 120
    for (i in 0..steps) {
        val t = i / steps.toFloat() * TWO_PI
        val r = baseRadius * (
            1f +
                0.07f * sin(3f * t + phase) +
                0.05f * sin(5f * t - phase * 1.3f) +
                0.03f * sin(7f * t + phase * 0.7f)
            )
        val x = center.x + r * cos(t)
        val y = center.y + r * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// ---------- Строка трека ----------

@Composable
private fun WaveTrackRow(
    track: WaveFeedTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        // Обложка-заглушка. Здесь будет AsyncImage или аналог
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        if (isCurrent) IridescentColors.take(4)
                        else listOf(AmoledSurfaceVariant, AmoledSurfaceVariant),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}