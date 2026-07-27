package com.example.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreviewView(
    previewView: PreviewView,
    cameraControl: CameraControl?,
    currentZoom: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }

    // Coroutine to dismiss the visual focus ring after 1.5 seconds
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            showFocusRing = true
            delay(1000)
            showFocusRing = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { view ->
                // Setup Gesture Listeners for tap to focus and pinch to zoom
                val scaleGestureDetector = ScaleGestureDetector(context,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            if (cameraControl == null) return false
                            val scaleFactor = detector.scaleFactor
                            val nextZoom = (currentZoom * scaleFactor).coerceIn(1.0f, 8.0f)
                            onZoomChanged(nextZoom)
                            cameraControl.setZoomRatio(nextZoom)
                            return true
                        }
                    }
                )

                val gestureDetector = GestureDetector(context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapUp(e: MotionEvent): Boolean {
                            if (cameraControl == null) return false
                            
                            val x = e.x
                            val y = e.y
                            focusPoint = Offset(x, y)
                            
                            val meteringPointFactory = view.meteringPointFactory
                            val point = meteringPointFactory.createPoint(x, y)
                            val action = FocusMeteringAction.Builder(point).build()
                            
                            Log.d("CameraPreviewView", "Focus requested at x: $x, y: $y")
                            cameraControl.startFocusAndMetering(action)
                            return true
                        }
                    }
                )

                view.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    if (!scaleGestureDetector.isInProgress) {
                        gestureDetector.onTouchEvent(event)
                    }
                    true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Level Reticle (matching professional camera viewfinder in reference image)
        if (!showFocusRing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val circleRadius = 55f
                val crossLength = 16f
                val levelLineLength = 22f

                // Faint white level ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = circleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )

                // Center '+' crosshair
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(cx - crossLength, cy),
                    end = Offset(cx + crossLength, cy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(cx, cy - crossLength),
                    end = Offset(cx, cy + crossLength),
                    strokeWidth = 2f
                )

                // Horizontal level tick lines on left and right of ring
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cx - circleRadius - levelLineLength - 6f, cy - 6f),
                    end = Offset(cx - circleRadius - 6f, cy - 6f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cx - circleRadius - levelLineLength - 6f, cy + 6f),
                    end = Offset(cx - circleRadius - 6f, cy + 6f),
                    strokeWidth = 2f
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cx + circleRadius + 6f, cy - 6f),
                    end = Offset(cx + circleRadius + levelLineLength + 6f, cy - 6f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(cx + circleRadius + 6f, cy + 6f),
                    end = Offset(cx + circleRadius + levelLineLength + 6f, cy + 6f),
                    strokeWidth = 2f
                )

                // Left crosshair mark
                val leftCx = cx - size.width * 0.28f
                val leftCy = cy - size.height * 0.08f
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(leftCx - 14f, leftCy),
                    end = Offset(leftCx + 14f, leftCy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(leftCx, leftCy - 14f),
                    end = Offset(leftCx, leftCy + 14f),
                    strokeWidth = 2f
                )
            }
        }

        // Focus Ring Overlay
        if (showFocusRing && focusPoint != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Outer ring
                drawCircle(
                    color = Color(0xFF00E676), // Neon green
                    radius = 50f,
                    center = focusPoint!!,
                    style = Stroke(width = 3f)
                )
                // Inner center point
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 8f,
                    center = focusPoint!!
                )
            }
        }
    }
}
