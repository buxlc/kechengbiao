package com.bu.kebiao.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

object BuEasing {
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Spring = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
}

object BuDuration {
    const val Short = 200
    const val Medium = 360
    const val Long = 520
    const val ExtraLong = 700
}
