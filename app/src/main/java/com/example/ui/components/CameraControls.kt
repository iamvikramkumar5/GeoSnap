package com.example.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

enum class CaptureMode {
    PHOTO, VIDEO
}

@Composable
fun CameraControls(
    captureMode: CaptureMode,
    onCaptureModeChanged: (CaptureMode) -> Unit,
    flashMode: Int,
    onFlashCycled: () -> Unit,
    zoomRatio: Float,
    onZoomSelected: (Float) -> Unit,
    isRecording: Boolean,
    isRecordingPaused: Boolean,
    recordingDurationSeconds: Int,
    onCaptureClicked: () -> Unit,
    onRecordClicked: () -> Unit,
    onPauseRecordClicked: () -> Unit,
    onResumeRecordClicked: () -> Unit,
    onStopRecordClicked: () -> Unit,
    onSwitchCameraClicked: () -> Unit,
    latestMediaFile: File?,
    onGalleryClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    isCapturing: Boolean = false,
    isShutterSoundEnabled: Boolean = true,
    onShutterSoundToggled: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // 1. Zoom and Secondary Option Toolbar (Minimalist and clean)
        if (!isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Mode Button
                IconButton(
                    onClick = onFlashCycled,
                    modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Cycle Flash Mode",
                        tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White else Color(0xFF00E676)
                    )
                }

                // Shutter Sound Toggle Button
                IconButton(
                    onClick = { onShutterSoundToggled(!isShutterSoundEnabled) },
                    modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isShutterSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Shutter Sound",
                        tint = if (isShutterSoundEnabled) Color(0xFF00E676) else Color.White
                    )
                }

                // Zoom Selector: 1x, 2x, 4x, 8x
                Row(
                    modifier = Modifier
                        .background(Color.DarkGray.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    listOf(1.0f, 2.0f, 4.0f, 8.0f).forEach { zoom ->
                        val isSelected = zoomRatio == zoom
                        Text(
                            text = "${zoom.toInt()}x",
                            color = if (isSelected) Color(0xFF00E676) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onZoomSelected(zoom) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Settings Gear Button
                IconButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // 2. Chronometer / Video Record HUD (Shown while recording)
        AnimatedVisibility(visible = isRecording) {
            val minutes = recordingDurationSeconds / 60
            val seconds = recordingDurationSeconds % 60
            val timeText = String.format("%02d:%02d", minutes, seconds)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .background(Color.Red.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.Red, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val dotAlpha by pulseTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .scale(dotAlpha)
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REC $timeText",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 3. Primary Capture Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Item: Gallery Button or Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = 0.4f))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable { onGalleryClicked() },
                contentAlignment = Alignment.Center
            ) {
                if (latestMediaFile != null) {
                    AsyncImage(
                        model = latestMediaFile,
                        contentDescription = "Open Gallery",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Open Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center Item: Shutter / Record Button
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .scale(scaleFactor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isCapturing && !isRecording
                    ) {
                        if (captureMode == CaptureMode.PHOTO) {
                            onCaptureClicked()
                        } else {
                            if (!isRecording) {
                                onRecordClicked()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (captureMode == CaptureMode.PHOTO) {
                    // Photo Shutter: No progress indicator inside (instant shutter experience!)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                } else {
                    // Video Trigger Styling
                    if (!isRecording) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(4.dp, Color.White, CircleShape)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    } else {
                        // Recording Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(4.dp, Color.White, CircleShape)
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isRecordingPaused) onResumeRecordClicked() else onPauseRecordClicked()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = "Pause/Resume Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onStopRecordClicked
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Recording",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right Item: Switch Camera Selector
            IconButton(
                onClick = onSwitchCameraClicked,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.DarkGray.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch Camera Lens",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 4. Capture Mode Switch Slider (PHOTO / VIDEO toggle)
        if (!isRecording) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .background(Color.DarkGray.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(2.dp)
            ) {
                CaptureMode.values().forEach { mode ->
                    val isSelected = captureMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isSelected) Color(0xFF00E676) else Color.Transparent)
                            .clickable { onCaptureModeChanged(mode) }
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
