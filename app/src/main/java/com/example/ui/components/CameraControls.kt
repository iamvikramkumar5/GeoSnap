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
fun CameraTopBar(
    flashMode: Int,
    onFlashCycled: () -> Unit,
    isShutterSoundEnabled: Boolean,
    onShutterSoundToggled: (Boolean) -> Unit,
    onSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Mode Icon
            IconButton(
                onClick = onFlashCycled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Cycle Flash Mode",
                    tint = if (flashMode == ImageCapture.FLASH_MODE_OFF) Color.White else Color(0xFF00E676),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Shutter Sound Toggle Button
            IconButton(
                onClick = { onShutterSoundToggled(!isShutterSoundEnabled) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isShutterSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle Shutter Sound",
                    tint = if (isShutterSoundEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings Gear Button
            IconButton(
                onClick = onSettingsClicked,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
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
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // 1. Zoom Selector Bar & Grid Icon (Pill capsule + grid button as seen in reference image)
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1A1A1A).copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val zoomLevels = listOf(
                            1.0f to "1x",
                            2.0f to "2x",
                            4.0f to "4x",
                            8.0f to "8x"
                        )
                        zoomLevels.forEach { (zoom, label) ->
                            val isSelected = zoomRatio == zoom
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable { onZoomSelected(zoom) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Grid / Options button next to zoom pill
                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1A1A1A).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid / Settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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

        // 4. Camera Mode Row (PHOTO, VIDEO, MORE)
        if (!isRecording) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modeList = listOf("PHOTO", "VIDEO", "MORE")
                modeList.forEach { modeName ->
                    val isSelected = when (modeName) {
                        "PHOTO" -> captureMode == CaptureMode.PHOTO
                        "VIDEO" -> captureMode == CaptureMode.VIDEO
                        else -> false
                    }
                    val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f)
                    
                    Box(
                        modifier = Modifier
                            .clickable {
                                when (modeName) {
                                    "PHOTO" -> onCaptureModeChanged(CaptureMode.PHOTO)
                                    "VIDEO" -> onCaptureModeChanged(CaptureMode.VIDEO)
                                    "MORE" -> onSettingsClicked()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeName,
                            color = textColor,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
