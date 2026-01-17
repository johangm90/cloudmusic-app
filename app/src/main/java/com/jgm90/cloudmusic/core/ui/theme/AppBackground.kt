package com.jgm90.cloudmusic.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val gradient = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF0B1118),
                Color(0xFF132733),
                Color(0xFF0C2E2C)
            )
        )
    }
    val glow = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF5EEAD4).copy(alpha = 0.35f),
                Color.Transparent
            )
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-80).dp, y = 40.dp)
                .background(glow, CircleShape)
                .alpha(0.55f)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 210.dp, y = 520.dp)
                .background(glow, CircleShape)
                .alpha(0.45f)
        )
        content()
    }
}
