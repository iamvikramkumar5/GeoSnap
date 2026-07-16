package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.CameraViewModel
import com.example.ui.components.CameraControls
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.CaptureMode
import com.example.ui.components.GPSOverlayCard
import com.example.ui.components.GalleryView
import com.example.ui.components.SettingsView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var viewModel: CameraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel = CameraViewModel(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    GeoSnapApp(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopLocationUpdates()
    }
}

@Composable
fun GeoSnapApp(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Branded splash screen display
    var showSplash by remember { mutableStateOf(true) }

    // Permission Tracking State
    val requiredPermissions = remember {
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
    }

    var permissionsGranted by remember {
        mutableStateOf(hasRequiredPermissions(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = hasRequiredPermissions(context)
                if (granted != permissionsGranted) {
                    permissionsGranted = granted
                    if (granted) {
                        viewModel.startLocationUpdates()
                        viewModel.loadSavedMedia()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val cameraGranted = permissionsMap[Manifest.permission.CAMERA] ?: false
        val locationGranted = (permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                (permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)

        permissionsGranted = cameraGranted && locationGranted
        
        if (permissionsGranted) {
            viewModel.startLocationUpdates()
            viewModel.loadSavedMedia()
        } else {
            Toast.makeText(context, "Camera & Location permissions are required for GPS Camera", Toast.LENGTH_LONG).show()
        }
    }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        if (permissionsGranted) {
            CameraWorkspace(viewModel = viewModel)
        } else {
            PermissionOnboardingScreen(
                onRequestPermissions = {
                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                }
            )
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // 2 seconds
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E222B), Color(0xFF0A0C10)),
                    radius = 1500f
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📍",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "GeoSnap Camera",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Capture Proof with Precision",
                color = Color(0xFF00E676),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Ads • Fast • Secure",
                color = Color.Gray,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }

        Text(
            text = "Designed with ❤️ by Vikram Kumar",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
fun PermissionOnboardingScreen(
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E12))
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "GeoSnap",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SECURE TELEMETRY CAMERA",
                color = Color(0xFF00E676),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PermissionRequirementItem(
                icon = Icons.Default.PhotoCamera,
                title = "Live Camera Sensor",
                description = "Enables high-resolution viewfinders, focal adjustments, and crisp image capture formats."
            )
            PermissionRequirementItem(
                icon = Icons.Default.LocationOn,
                title = "Satellite GPS Coordinates",
                description = "Retrieves live latitude, longitude, and reverse geocodes full street-level addresses instantly."
            )
            PermissionRequirementItem(
                icon = Icons.Default.Mic,
                title = "High Fidelity Microphone",
                description = "Captures immersive sound layers alongside Full HD video recording outputs."
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "GRANT ACCESS",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            ) {
                Text(
                    text = "Configure in App Settings",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun PermissionRequirementItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CameraWorkspace(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    
    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.FHD))
            .build()
        VideoCapture.withOutput(recorder)
    }

    val cameraSelector by viewModel.cameraSelector.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    
    val gpsData by viewModel.gpsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val mapBitmap by viewModel.mapBitmap.collectAsState()
    val azimuth by viewModel.azimuth.collectAsState()
    val deviceOrientation by viewModel.deviceOrientation.collectAsState()

    val isCapturing by viewModel.isCapturing.collectAsState()
    val shutterFlashActive by viewModel.shutterFlashActive.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val isRecordingPaused by viewModel.isRecordingPaused.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val savedMedia by viewModel.savedMedia.collectAsState()

    var activeCaptureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var showGallery by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                Log.e("CameraWorkspace", "Error unbinding CameraX on dispose", e)
            }
        }
    }

    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(cameraSelector, activeCaptureMode, cameraProviderFuture, lifecycleState) {
        val cameraProvider = cameraProviderFuture.get()
        
        if (lifecycleState != Lifecycle.State.RESUMED) {
            try {
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraWorkspace", "Error unbinding CameraX on lifecycle pause", e)
            }
            return@LaunchedEffect
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        try {
            cameraProvider.unbindAll()
            
            val camera = if (activeCaptureMode == CaptureMode.PHOTO) {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } else {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            }
            activeCamera = camera
            
            camera.cameraControl.setZoomRatio(zoomRatio)
        } catch (e: Exception) {
            Log.e("CameraWorkspace", "CameraX UseCase binding failed", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Camera Preview (Tap-to-focus and Pinch-to-zoom integrated)
        CameraPreviewView(
            previewView = previewView,
            cameraControl = activeCamera?.cameraControl,
            currentZoom = zoomRatio,
            onZoomChanged = { viewModel.setZoom(it) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Real-time GPS Watermark Overlay card (Bottom-aligned column, GCam-style!)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            ) {
                GPSOverlayCard(
                    gpsData = gpsData,
                    settings = settings,
                    mapBitmap = mapBitmap,
                    azimuth = azimuth
                )
            }

            CameraControls(
                captureMode = activeCaptureMode,
                onCaptureModeChanged = { activeCaptureMode = it },
                flashMode = flashMode,
                onFlashCycled = { viewModel.cycleFlash() },
                zoomRatio = zoomRatio,
                onZoomSelected = {
                    viewModel.setZoom(it)
                    activeCamera?.cameraControl?.setZoomRatio(it)
                },
                isRecording = isRecording,
                isRecordingPaused = isRecordingPaused,
                recordingDurationSeconds = recordingDuration,
                onCaptureClicked = {
                    imageCapture.flashMode = flashMode
                    viewModel.takePhoto(imageCapture, context) {
                        // Toast removed as requested by the user
                    }
                },
                onRecordClicked = {
                    viewModel.startVideoRecording(videoCapture, context) {
                        // Toast removed as requested by the user
                    }
                },
                onPauseRecordClicked = { viewModel.pauseVideoRecording() },
                onResumeRecordClicked = { viewModel.resumeVideoRecording() },
                onStopRecordClicked = { viewModel.stopVideoRecording() },
                onSwitchCameraClicked = { viewModel.toggleCamera() },
                latestMediaFile = savedMedia.firstOrNull(),
                onGalleryClicked = { showGallery = true },
                onSettingsClicked = { showSettings = true },
                isCapturing = isCapturing,
                isShutterSoundEnabled = settings.shutterSound,
                onShutterSoundToggled = { enabled ->
                    viewModel.updateSettings { copy(shutterSound = enabled) }
                }
            )
        }

        // 3. Crisp quick shutter dip overlay (resembling mechanical camera shutter)
        AnimatedVisibility(
            visible = shutterFlashActive,
            enter = fadeIn(animationSpec = tween(50)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black) // instant mechanical dip to black
            )
        }

        // 4. Slide-Up Media Browser Overlay
        AnimatedVisibility(
            visible = showGallery,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            GalleryView(
                mediaFiles = savedMedia,
                onClose = { showGallery = false },
                onDeleteFile = { file ->
                    file.delete()
                    viewModel.loadSavedMedia()
                }
            )
        }

        // 5. Slide-Up Settings Dashboard Modal Overlay
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            SettingsView(
                settings = settings,
                onSettingsChanged = { updated ->
                    viewModel.updateSettings { updated }
                },
                onReset = {
                    viewModel.resetSettings()
                },
                onClose = {
                    showSettings = false
                }
            )
        }
    }
}

private fun hasRequiredPermissions(context: Context): Boolean {
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return cameraGranted && locationGranted
}
