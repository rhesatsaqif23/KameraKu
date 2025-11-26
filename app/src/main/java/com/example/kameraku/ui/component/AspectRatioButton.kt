package com.example.kameraku.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kameraku.camera.AspectOption

@Composable
fun AspectRatioButton(
    current: AspectOption,
    onToggle: (AspectOption) -> Unit
) {
    val next = if (current == AspectOption.RATIO_4_3)
        AspectOption.RATIO_16_9
    else
        AspectOption.RATIO_4_3

    Text(
        text = current.label,
        color = Color.White,
        modifier = Modifier
            .clickable { onToggle(next) }
            .padding(6.dp)
    )
}

