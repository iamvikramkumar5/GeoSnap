package com.example.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    aspectRatio: String = "3:4",
    onAspectRatioCycled: () -> Unit = {},
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
            .background(Color.Transparent)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aspect Ratio Selector Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onAspectRatioCycled() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = aspectRatio,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

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
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // 1. Zoom Selector Bar & Grid Icon (Pill capsule + grid button)
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
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

        // 4. Primary Capture Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
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
                    // Photo Shutter
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

            // Right Item: Switch Camera Selector with tactile spring flip animation
            var cameraSwitchRotation by remember { mutableFloatStateOf(0f) }
            var cameraSwitchScale by remember { mutableFloatStateOf(1f) }

            val animatedSwitchRotation by animateFloatAsState(
                targetValue = cameraSwitchRotation,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "cameraSwitchRotation"
            )

            val animatedSwitchScale by animateFloatAsState(
                targetValue = cameraSwitchScale,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                finishedListener = { cameraSwitchScale = 1f },
                label = "cameraSwitchScale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = animatedSwitchScale
                        scaleY = animatedSwitchScale
                    }
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = 0.5f))
                    .clickable {
                        cameraSwitchRotation += 180f
                        cameraSwitchScale = 0.8f
                        onSwitchCameraClicked()
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Switch Camera Lens",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            rotationZ = animatedSwitchRotation
                        }
                )
            }
        }

        // 4. Apple Camera Style Floating Segmented Pill (PHOTO | VIDEO) - Below shutter button
        if (!isRecording) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modeList = listOf(CaptureMode.PHOTO to "PHOTO", CaptureMode.VIDEO to "VIDEO")
                modeList.forEach { (mode, modeName) ->
                    val isSelected = captureMode == mode

                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "modeBgColor"
                    )
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) Color.Black else Color.White,
                        animationSpec = tween(durationMillis = 200),
                        label = "modeTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(animatedBgColor)
                            .clickable { onCaptureModeChanged(mode) }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeName,
                            color = animatedTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}
