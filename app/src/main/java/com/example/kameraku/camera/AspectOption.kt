package com.example.kameraku.camera

import androidx.camera.core.AspectRatio

enum class AspectOption(val label: String, val ratio: Int) {
    RATIO_4_3("4:3", AspectRatio.RATIO_4_3),
    RATIO_16_9("16:9", AspectRatio.RATIO_16_9)
}
