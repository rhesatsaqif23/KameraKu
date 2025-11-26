package com.example.kameraku.ui.component

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kameraku.camera.takePhoto

@Composable
fun ShutterButton(
    imageCapture: ImageCapture?,
    previewView: PreviewView?,
    context: android.content.Context,
    onCaptured: (Uri?) -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(6.dp, Color.LightGray, CircleShape)
            .clickable {
                if (imageCapture != null && previewView != null) {
                    takePhoto(context, previewView, imageCapture, onCaptured)
                }
            }
    )
}