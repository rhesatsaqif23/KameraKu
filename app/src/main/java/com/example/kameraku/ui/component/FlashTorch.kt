package com.example.kameraku.ui.component

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FlashTorchColumn(
    torchOn: Boolean,
    flashMode: Int,
    onToggleTorch: () -> Unit,
    onCycleFlash: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlashButton(flashMode, onCycleFlash)
        TorchButton(torchOn, onToggleTorch)
    }
}

@Composable
fun FlashButton(flashMode: Int, onCycle: () -> Unit) {
    val icon = when (flashMode) {
        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
        else -> Icons.Default.FlashOff
    }

    Box(
        modifier = Modifier
            .size(45.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onCycle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Flash",
            tint = Color.White
        )
    }
}

@Composable
fun TorchButton(torchOn: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(45.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Highlight,
            contentDescription = "Torch",
            tint = if (torchOn) Color.Yellow else Color.White
        )
    }
}

// Flash cycle helper
fun nextFlash(current: Int): Int {
    return when (current) {
        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
        else -> ImageCapture.FLASH_MODE_OFF
    }
}