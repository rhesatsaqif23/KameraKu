package com.example.kameraku.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(onPreviewReady: (PreviewView) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            PreviewView(c).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                scaleType = PreviewView.ScaleType.FIT_CENTER
                post { onPreviewReady(this) }
            }
        }
    )
}

