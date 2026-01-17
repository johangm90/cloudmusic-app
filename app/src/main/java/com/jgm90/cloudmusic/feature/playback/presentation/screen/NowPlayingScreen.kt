package com.jgm90.cloudmusic.feature.playback.presentation.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jgm90.cloudmusic.feature.playback.presentation.component.AlbumArtWithVisualizer
import com.jgm90.cloudmusic.feature.playback.presentation.component.LyricsDisplay
import com.jgm90.cloudmusic.feature.playback.presentation.component.NowPlayingTopBar
import com.jgm90.cloudmusic.feature.playback.presentation.component.OptimizedSeekBar
import com.jgm90.cloudmusic.feature.playback.presentation.component.PlaybackControls
import com.jgm90.cloudmusic.feature.playback.presentation.component.visualizer.AmbientBackground
import com.jgm90.cloudmusic.feature.playback.presentation.component.visualizer.ParticleSystem
import com.jgm90.cloudmusic.feature.playback.presentation.state.AmbientColors
import com.jgm90.cloudmusic.feature.playback.presentation.state.NowPlayingAction
import com.jgm90.cloudmusic.feature.playback.presentation.viewmodel.NowPlayingViewModel
import com.jgm90.cloudmusic.feature.settings.domain.model.ParticleLevel
import com.jgm90.cloudmusic.feature.settings.domain.model.VisualizerStyle

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    val infiniteTransition = rememberInfiniteTransition(label = "simulatedBeat")
    val simulatedBeat by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "simulatedBeat"
    )

    val effectiveBeat = if (uiState.hasAudioPermission) uiState.beatLevel else simulatedBeat
    val playingBeat = if (uiState.isPlaying) effectiveBeat else 0f
    val sliderMax = if (uiState.durationMs > 0) uiState.durationMs else 1

    // Use ambient colors based on setting
    val ambientColors = if (settings.ambientModeEnabled) {
        uiState.ambientColors
    } else {
        AmbientColors.Default
    }

    val accentGradient = Brush.linearGradient(
        listOf(
            ambientColors.accentPrimary,
            ambientColors.accentSecondary
        )
    )

    val softGlow = Brush.radialGradient(
        colors = listOf(
            ambientColors.accentPrimary.copy(alpha = 0.35f),
            Color.Transparent
        )
    )

    // Determine if visualizer should be shown
    val showVisualizer = settings.visualizerStyle != VisualizerStyle.NONE

    // Determine if particles should be shown and their count multiplier
    val showParticles = settings.particleLevel != ParticleLevel.NONE
    val particleIntensityMultiplier = when (settings.particleLevel) {
        ParticleLevel.NONE -> 0f
        ParticleLevel.LOW -> 0.5f
        ParticleLevel.MEDIUM -> 1f
        ParticleLevel.HIGH -> 1.5f
    }

    AmbientBackground(
        ambientColors = ambientColors,
        beatLevel = if (settings.ambientModeEnabled) playingBeat else 0f,
    ) {
        // Particles - only show if enabled
        if (showParticles) {
            ParticleSystem(
                modifier = Modifier.fillMaxSize(),
                intensity = playingBeat * particleIntensityMultiplier,
                enabled = uiState.isPlaying,
                colors = ambientColors.particleColors,
            )
        }

        // Decorative glow orbs - only show if ambient mode is enabled
        if (settings.ambientModeEnabled) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(x = (-90).dp, y = 40.dp)
                    .background(softGlow, CircleShape)
                    .alpha(0.6f)
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = 200.dp, y = 480.dp)
                    .background(softGlow, CircleShape)
                    .alpha(0.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar
            NowPlayingTopBar(
                isLiked = uiState.isLiked,
                onBack = onNavigateBack,
                onToggleLike = { viewModel.onAction(NowPlayingAction.ToggleLike) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Album art with visualizer
            AlbumArtWithVisualizer(
                coverUrl = uiState.coverUrl,
                isPlaying = uiState.isPlaying,
                beatLevel = playingBeat,
                visualizerBands = uiState.visualizerBands,
                particleColors = ambientColors.particleColors,
                accentGradient = accentGradient,
                showVisualizer = showVisualizer,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Song info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.songTitle.ifEmpty { "Reproduciendo" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.songArtist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Seek bar
            OptimizedSeekBar(
                progress = uiState.progressMs.coerceIn(0, sliderMax),
                max = sliderMax,
                onSeekStart = { viewModel.onAction(NowPlayingAction.StartSeeking) },
                onSeekChange = { viewModel.onAction(NowPlayingAction.UpdateProgress(it)) },
                onSeekEnd = { viewModel.onAction(NowPlayingAction.StopSeeking) },
                activeBrush = accentGradient,
                inactiveColor = Color.White.copy(alpha = 0.2f),
                beatLevel = playingBeat,
            )

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = uiState.elapsedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = uiState.durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback controls
            PlaybackControls(
                isPlaying = uiState.isPlaying,
                shuffleEnabled = uiState.shuffleEnabled,
                repeatMode = uiState.repeatMode,
                accentGradient = accentGradient,
                onPlayPause = { viewModel.onAction(NowPlayingAction.PlayPause) },
                onPrevious = { viewModel.onAction(NowPlayingAction.SkipToPrevious) },
                onNext = { viewModel.onAction(NowPlayingAction.SkipToNext) },
                onShuffle = { viewModel.onAction(NowPlayingAction.ToggleShuffle) },
                onRepeat = { viewModel.onAction(NowPlayingAction.ToggleRepeat) },
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Lyrics
            LyricsDisplay(
                currentLyric = uiState.currentLyric,
                nextLyric = uiState.nextLyric,
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
