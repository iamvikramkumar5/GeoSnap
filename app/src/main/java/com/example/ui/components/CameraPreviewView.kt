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
