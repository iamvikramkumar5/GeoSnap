package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.GPSData
import com.example.model.SettingsData
import com.example.service.LocationService
import com.example.utils.MediaSaver
import com.example.utils.SettingsManager
import com.example.utils.WatermarkGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraViewModel(private val context: Context) : ViewModel(), SensorEventListener {
    private val TAG = "CameraViewModel"
    private val locationService = LocationService(context)
    val settingsManager = SettingsManager(context)

    // Settings StateFlow
    val settingsState: StateFlow<SettingsData> = settingsManager.settings

    // Combined Live GPS and Manual Override state
    val gpsState: StateFlow<GPSData> = combine(
        locationService.gpsState,
        settingsState
    ) { gps, settings ->
        if (settings.useManualLocation) {
            gps.copy(
                latitude = settings.manualLatitude,
                longitude = settings.manualLongitude,
                address = settings.manualAddress,
                city = settings.manualCity,
                country = settings.manualCountry,
                shortAddress = settings.manualCity.ifEmpty { "Manual Location" }
            )
        } else {
            gps
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GPSData())

    // Cached pre-fetched static map tile
    private val _mapBitmap = MutableStateFlow<Bitmap?>(null)
    val mapBitmap: StateFlow<Bitmap?> = _mapBitmap.asStateFlow()

    private var mapFetchJob: Job? = null
    private var lastFetchedLat = 0.0
    private var lastFetchedLng = 0.0
    private var lastFetchedType = ""

    // Dynamic Physical Compass Azimuth
    private var sensorManager: SensorManager? = null
    private var compassSensor: Sensor? = null
    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> = _azimuth.asStateFlow()

    // Camera Settings States
    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    // Shutter Trigger Animation State
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // Video Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isRecordingPaused = MutableStateFlow(false)
    val isRecordingPaused: StateFlow<Boolean> = _isRecordingPaused.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private var activeRecording: Recording? = null
    private var timerJob: Job? = null

    // Saved Media Gallery State
    private val _savedMedia = MutableStateFlow<List<File>>(emptyList())
    val savedMedia: StateFlow<List<File>> = _savedMedia.asStateFlow()

    // Simulator GPS toggle
    private val _isSimulatorMode = MutableStateFlow(false)
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    // Shutter Flash active state for Compose animation
    private val _shutterFlashActive = MutableStateFlow(false)
    val shutterFlashActive: StateFlow<Boolean> = _shutterFlashActive.asStateFlow()

    private var recordingStartTime = 0L
    private var orientationListener: android.view.OrientationEventListener? = null
    private val _deviceOrientation = MutableStateFlow(0)
    val deviceOrientation: StateFlow<Int> = _deviceOrientation.asStateFlow()

    init {
        // Initialize sensors
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        @Suppress("DEPRECATION")
        compassSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        registerCompass()

        // Automatically start fetching location and loading files
        startLocationUpdates()
        loadSavedMedia()

        orientationListener = object : android.view.OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return
                val newRotation = when (orientation) {
                    in 45 until 135 -> 270
                    in 135 until 225 -> 180
                    in 225 until 315 -> 90
                    else -> 0
                }
                if (newRotation != _deviceOrientation.value) {
                    _deviceOrientation.value = newRotation
                    Log.d(TAG, "Device orientation changed: $newRotation")
                }
            }
        }
        orientationListener?.enable()

        // Launch auto background downloader for google static maps whenever coordinates or map style change!
        viewModelScope.launch {
            combine(gpsState, settingsState) { gps, settings ->
                Pair(gps, settings)
            }.collect { (gps, settings) ->
                triggerMapFetch(gps.latitude, gps.longitude, settings.mapViewType)
            }
        }
    }

    private fun registerCompass() {
        compassSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun unregisterCompass() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        @Suppress("DEPRECATION")
        if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            val degree = event.values[0]
            _azimuth.value = degree
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerMapFetch(lat: Double, lng: Double, mapViewType: String) {
        if (lat == 0.0 && lng == 0.0) return
        val distance = Math.sqrt(Math.pow(lat - lastFetchedLat, 2.0) + Math.pow(lng - lastFetchedLng, 2.0))
        if (distance < 0.00015 && _mapBitmap.value != null && mapViewType == lastFetchedType) {
            return // Skip if coordinates are virtually identical to cache to preserve data and battery!
        }

        mapFetchJob?.cancel()
        mapFetchJob = viewModelScope.launch(Dispatchers.IO) {
            val bitmap = WatermarkGenerator.fetchMapBitmap(lat, lng, mapViewType)
            if (bitmap != null) {
                _mapBitmap.value = bitmap
                lastFetchedLat = lat
                lastFetchedLng = lng
                lastFetchedType = mapViewType
            }
        }
    }

    fun startLocationUpdates() {
        if (_isSimulatorMode.value) {
            locationService.startSimulatorMode()
        } else {
            locationService.startLocationUpdates()
        }
    }

    fun stopLocationUpdates() {
        locationService.stopLocationUpdates()
    }

    fun toggleSimulatorMode() {
        _isSimulatorMode.value = !_isSimulatorMode.value
        startLocationUpdates()
    }

    fun toggleCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun cycleFlash() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
    }

    fun setZoom(ratio: Float) {
        _zoomRatio.value = ratio
    }

    fun updateSettings(updater: SettingsData.() -> SettingsData) {
        settingsManager.updateSetting(updater)
    }

    fun resetSettings() {
        settingsManager.resetSettings()
    }

    fun loadSavedMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val albumName = "GeoSnap" // Pictures/GeoSnap and Movies/GeoSnap!
            val photosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName)
            val videosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), albumName)

            val mediaFiles = mutableListOf<File>()
            
            if (photosDir.exists() && photosDir.isDirectory) {
                photosDir.listFiles { file -> 
                    file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true)) 
                }?.let { mediaFiles.addAll(it) }
            }

            if (videosDir.exists() && videosDir.isDirectory) {
                videosDir.listFiles { file -> 
                    file.isFile && file.extension.equals("mp4", true) 
                }?.let { mediaFiles.addAll(it) }
            }

            mediaFiles.sortByDescending { file -> file.lastModified() }
            _savedMedia.value = mediaFiles
        }
    }

    fun takePhoto(imageCapture: ImageCapture, context: Context, onSuccess: () -> Unit) {
        if (_isCapturing.value) return
        _isCapturing.value = true
        _shutterFlashActive.value = true

        viewModelScope.launch {
            delay(100) // very short, crisp dip to black shutter simulation
            _shutterFlashActive.value = false
        }

        val settings = settingsState.value

        // Shutter Sound Cue
        if (settings.shutterSound) {
            try {
                val sound = android.media.MediaActionSound()
                sound.play(android.media.MediaActionSound.SHUTTER_CLICK)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing shutter sound", e)
            }
        }

        // Crisp quick haptic tap
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(30)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }

        val cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()

                    // Free up capture state instantly so user can snap subsequent photos with zero block!
                    _isCapturing.value = false

                    // Process and watermark in background thread
                    viewModelScope.launch(Dispatchers.Default) {
                        try {
                            val orientedBitmap = if (rotation != 0) {
                                rotateBitmap(bitmap, rotation)
                            } else {
                                bitmap
                            }

                            val gps = gpsState.value
                            val cachedMap = _mapBitmap.value // read pre-fetched map tile instantly (no blocking network calls!)
                            val activeAzi = _azimuth.value

                            // Draw watermark using standard rendering engine
                            val watermarked = WatermarkGenerator.drawWatermark(orientedBitmap, gps, settings, cachedMap, activeAzi)

                            // Save SINGLE file
                            MediaSaver.savePhoto(context, watermarked, gps, settings)

                            // Reload Gallery
                            loadSavedMedia()

                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to process photo capture", e)
                        } finally {
                            cameraExecutor.shutdown()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "CameraX takePicture failed", exception)
                    _isCapturing.value = false
                    cameraExecutor.shutdown()
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun startVideoRecording(videoCapture: VideoCapture<Recorder>, context: Context, onSuccess: () -> Unit) {
        if (_isRecording.value) return

        recordingStartTime = System.currentTimeMillis()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tempFile = File(context.cacheDir, "GPS_REC_$timestamp.mp4")
        val outputOptions = FileOutputOptions.Builder(tempFile).build()

        val pendingRecording = videoCapture.output
            .prepareRecording(context, outputOptions)

        val hasMicrophone = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasMicrophone) {
            pendingRecording.withAudioEnabled()
        }

        _isRecording.value = true
        _isRecordingPaused.value = false
        _recordingDurationSeconds.value = 0

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    startRecordingTimer()
                }
                is VideoRecordEvent.Pause -> {
                    _isRecordingPaused.value = true
                    stopRecordingTimer()
                }
                is VideoRecordEvent.Resume -> {
                    _isRecordingPaused.value = false
                    startRecordingTimer()
                }
                is VideoRecordEvent.Finalize -> {
                    _isRecording.value = false
                    _isRecordingPaused.value = false
                    stopRecordingTimer()

                    val targetFile = tempFile
                    if (!recordEvent.hasError()) {
                        val gps = gpsState.value
                        val recStart = recordingStartTime
                        val settings = settingsState.value
                        val cachedMap = _mapBitmap.value // read pre-fetched map instantly!
                        val activeAzi = _azimuth.value

                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                MediaSaver.saveVideo(
                                    context = context,
                                    tempFile = targetFile,
                                    gpsData = gps,
                                    settings = settings,
                                    mapBitmap = cachedMap,
                                    recordingStartTime = recStart,
                                    azimuth = activeAzi
                                ) { finalUri ->
                                    viewModelScope.launch(Dispatchers.Main) {
                                        loadSavedMedia()
                                        onSuccess()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to transcode recorded video", e)
                                if (targetFile.exists()) {
                                    targetFile.delete()
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Video finalized with error: ${recordEvent.error}")
                        if (targetFile.exists()) {
                            targetFile.delete()
                        }
                    }
                    activeRecording = null
                }
            }
        }
    }

    fun pauseVideoRecording() {
        activeRecording?.pause()
    }

    fun resumeVideoRecording() {
        activeRecording?.resume()
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
    }

    private fun startRecordingTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }
    }

    private fun stopRecordingTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val planeProxy = imageProxy.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun rotateBitmap(source: Bitmap, angle: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun onCleared() {
        super.onCleared()
        unregisterCompass()
        try {
            orientationListener?.disable()
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling orientation listener", e)
        }
        stopLocationUpdates()
        stopRecordingTimer()
    }
}
