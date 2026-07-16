package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    // Elegant dynamic animated drifting for compass fallback/simulation
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
    
    // Default template card height is around 120dp. Simple template is smaller (around 48dp).
    val cardHeightDp = if (settings.template == com.example.model.WatermarkTemplate.SIMPLE) {
        48.dp * sizeMultiplier
    } else {
        132.dp * sizeMultiplier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeightDp)
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


