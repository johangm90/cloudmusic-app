package com.jgm90.cloudmusic.feature.settings.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jgm90.cloudmusic.core.ui.theme.AppBackground
import com.jgm90.cloudmusic.feature.settings.domain.model.ParticleLevel
import com.jgm90.cloudmusic.feature.settings.domain.model.ShaderQuality
import com.jgm90.cloudmusic.feature.settings.domain.model.VisualizerStyle
import com.jgm90.cloudmusic.feature.settings.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(text = "Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSection(title = "Visual Effects") {
                    SwitchSettingItem(
                        title = "Ambient Mode",
                        description = "Dynamic background colors from album art",
                        checked = settings.ambientModeEnabled,
                        onCheckedChange = { viewModel.setAmbientMode(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OptionSettingItem(
                        title = "Visualizer Style",
                        selectedOption = settings.visualizerStyle.displayName,
                        options = VisualizerStyle.entries.map { it.displayName },
                        onOptionSelected = { index ->
                            viewModel.setVisualizerStyle(VisualizerStyle.entries[index])
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OptionSettingItem(
                        title = "Particles",
                        selectedOption = settings.particleLevel.displayName,
                        options = ParticleLevel.entries.map { it.displayName },
                        onOptionSelected = { index ->
                            viewModel.setParticleLevel(ParticleLevel.entries[index])
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OptionSettingItem(
                        title = "Shader Quality",
                        selectedOption = settings.shaderQuality.displayName,
                        options = ShaderQuality.entries.map { it.displayName },
                        onOptionSelected = { index ->
                            viewModel.setShaderQuality(ShaderQuality.entries[index])
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            )
        )
    }
}

@Composable
private fun OptionSettingItem(
    title: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selectedOption
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { onOptionSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private val VisualizerStyle.displayName: String
    get() = when (this) {
        VisualizerStyle.WAVE_RING -> "Wave Ring"
        VisualizerStyle.NONE -> "None"
    }

private val ParticleLevel.displayName: String
    get() = when (this) {
        ParticleLevel.NONE -> "None"
        ParticleLevel.LOW -> "Low"
        ParticleLevel.MEDIUM -> "Medium"
        ParticleLevel.HIGH -> "High"
    }

private val ShaderQuality.displayName: String
    get() = when (this) {
        ShaderQuality.LOW -> "Low"
        ShaderQuality.MEDIUM -> "Medium"
        ShaderQuality.HIGH -> "High"
    }
