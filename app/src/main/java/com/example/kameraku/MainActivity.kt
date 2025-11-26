package com.example.kameraku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.example.kameraku.ui.CameraScreen
import com.example.kameraku.ui.theme.KameraKuTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KameraKuTheme {
                CameraScreen()
            }
        }
    }
}
