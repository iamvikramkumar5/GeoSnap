package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.model.GPSData
import com.example.model.SettingsData
import com.example.utils.WatermarkGenerator

@Composable
fun GPSOverlayCard(
    gpsData: GPSData,
    settings: SettingsData,
    mapBitmap: Bitmap?,
    azimuth: Float,
    deviceOrientation: Int = 0,
    modifier: Modifier = Modifier
) {
    // Real-time orientation rotation target (smooth 200ms transition)
    val targetRotationZ = when (deviceOrientation) {
        90 -> 90f
        270 -> -90f
        else -> 0f
    }

    val animRotationZ by animateFloatAsState(
        targetValue = targetRotationZ,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "watermark_rotation"
    )

    // Dynamic animated drifting for compass fallback/simulation
    val infiniteTransition = rememberInfiniteTransition(label = "compass_drift")
    val simulatedAzimuth by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    val activeAzimuth = if (azimuth == 0f) simulatedAzimuth else azimuth

    // Calculate dynamic aspect ratio height in DP based on settings watermarkSize
    val sizeMultiplier = when (settings.watermarkSize) {
        "Small" -> 0.75f
        "Large" -> 1.25f
        else -> 1.0f
    }
    
    val cardHeightDp = if (settings.template == com.example.model.WatermarkTemplate.SIMPLE) {
        48.dp * sizeMultiplier
    } else {
        132.dp * sizeMultiplier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeightDp)
            .graphicsLayer {
                rotationZ = animRotationZ
                val w = size.width
                val h = size.height
                val diff = (w - h) / 2f
                val marginPx = 12.dp.toPx()

                when {
                    animRotationZ > 0f -> {
                        // Landscape (device 90deg -> rotation +90deg)
                        val progress = (animRotationZ / 90f).coerceIn(0f, 1f)
                        translationX = progress * (-diff + marginPx)
                        translationY = progress * (-diff)
                    }
                    animRotationZ < 0f -> {
                        // Landscape (device 270deg -> rotation -90deg)
                        val progress = (animRotationZ / -90f).coerceIn(0f, 1f)
                        translationX = progress * (diff - marginPx)
                        translationY = progress * (-diff)
                    }
                    else -> {
                        translationX = 0f
                        translationY = 0f
                    }
                }
            }
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        WatermarkGenerator.drawWatermarkOnCanvas(
            canvas = nativeCanvas,
            gpsData = gpsData,
            settings = settings,
            mapBitmap = mapBitmap,
            width = size.width,
            height = size.height,
            azimuth = activeAzimuth
        )
    }
}



