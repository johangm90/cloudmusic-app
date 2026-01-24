package com.jgm90.cloudmusic.feature.playback.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jgm90.cloudmusic.core.ui.theme.AppBackground
import com.jgm90.cloudmusic.feature.playback.presentation.component.AlbumArtWithVisualizer
import com.jgm90.cloudmusic.feature.playback.presentation.component.LyricsDisplay
import com.jgm90.cloudmusic.feature.playback.presentation.component.NowPlayingTopBar
import com.jgm90.cloudmusic.feature.playback.presentation.component.OptimizedSeekBar
import com.jgm90.cloudmusic.feature.playback.presentation.component.PlaybackControls
import com.jgm90.cloudmusic.feature.playback.presentation.state.NowPlayingAction
import com.jgm90.cloudmusic.feature.playback.presentation.viewmodel.NowPlayingViewModel
import com.jgm90.cloudmusic.feature.settings.domain.model.VisualizerStyle

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val sliderMax = if (uiState.durationMs > 0) uiState.durationMs else 1
    val showVisualizer = settings.visualizerStyle != VisualizerStyle.NONE
    val effectiveBeat = if (uiState.hasAudioPermission) uiState.beatLevel else 0f
    val playingBeat = if (uiState.isPlaying) effectiveBeat else 0f
    val accentColor = MaterialTheme.colorScheme.primary
    val isLoading = uiState.isLoading

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NowPlayingTopBar(
                isLiked = uiState.isLiked,
                onBack = onNavigateBack,
                onToggleLike = { viewModel.onAction(NowPlayingAction.ToggleLike) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            AlbumArtWithVisualizer(
                coverUrl = uiState.coverUrl,
                isPlaying = uiState.isPlaying,
                beatLevel = playingBeat,
                visualizerBands = uiState.visualizerBands,
                showVisualizer = showVisualizer,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.songTitle.ifEmpty { "Reproduciendo" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.songArtist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "Cargando...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OptimizedSeekBar(
                progress = uiState.progressMs.coerceIn(0, sliderMax),
                max = sliderMax,
                onSeekStart = { viewModel.onAction(NowPlayingAction.StartSeeking) },
                onSeekChange = { viewModel.onAction(NowPlayingAction.UpdateProgress(it)) },
                onSeekEnd = { viewModel.onAction(NowPlayingAction.StopSeeking) },
                activeColor = accentColor,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                enabled = !isLoading,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = uiState.elapsedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PlaybackControls(
                isPlaying = uiState.isPlaying,
                shuffleEnabled = uiState.shuffleEnabled,
                repeatMode = uiState.repeatMode,
                accentColor = accentColor,
                isLoading = isLoading,
                enabled = !isLoading,
                onPlayPause = { viewModel.onAction(NowPlayingAction.PlayPause) },
                onPrevious = { viewModel.onAction(NowPlayingAction.SkipToPrevious) },
                onNext = { viewModel.onAction(NowPlayingAction.SkipToNext) },
                onShuffle = { viewModel.onAction(NowPlayingAction.ToggleShuffle) },
                onRepeat = { viewModel.onAction(NowPlayingAction.ToggleRepeat) },
            )

            Spacer(modifier = Modifier.height(18.dp))

            LyricsDisplay(
                currentLyric = uiState.currentLyric,
                nextLyric = uiState.nextLyric,
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
