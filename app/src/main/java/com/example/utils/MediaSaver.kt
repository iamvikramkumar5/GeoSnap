package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Effect
import androidx.media3.transformer.Effects
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.example.model.GPSData
import com.example.model.SettingsData
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaSaver {
    private const val TAG = "MediaSaver"
    private const val ALBUM_NAME = "GeoSnap" // Automatically save into Pictures/GeoSnap and Movies/GeoSnap!

    fun savePhoto(
        context: Context,
        watermarkedBitmap: Bitmap,
        gpsData: GPSData,
        settings: SettingsData
    ): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayNameWatermarked = "GPS_IMG_${timestamp}.jpg"

        val watermarkedUri = saveBitmapToMediaStore(context, watermarkedBitmap, displayNameWatermarked, settings)

        // Save companion metadata json
        saveCompanionMetadata(context, displayNameWatermarked, gpsData)

        return watermarkedUri
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun saveVideo(
        context: Context,
        tempFile: File,
        gpsData: GPSData,
        settings: SettingsData,
        mapBitmap: Bitmap?,
        recordingStartTime: Long,
        azimuth: Float,
        onComplete: (Uri?) -> Unit
    ) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "GPS_VID_$timestamp.mp4"
        
        val outputTranscodedFile = File(context.cacheDir, "TRANSCODED_$displayName")
        
        try {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(tempFile))
            
            var videoWidth = 1920
            var videoHeight = 1080
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(tempFile.absolutePath)
                val widthStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                val rotation = rotationStr?.toIntOrNull() ?: 0
                val w = widthStr?.toIntOrNull() ?: 1920
                val h = heightStr?.toIntOrNull() ?: 1080
                
                if (rotation == 90 || rotation == 270) {
                    videoWidth = h
                    videoHeight = w
                } else {
                    videoWidth = w
                    videoHeight = h
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to retrieve video metadata for overlay", ex)
            } finally {
                try { retriever.release() } catch (ex2: Exception) {}
            }

            // Do not fetch map synchronously over the network during video processing (keep it offline/cached)
            val finalMapBitmap = mapBitmap

            // Construct and start transformer on the Main Looper thread as strictly required by Media3
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    val overlay = VideoWatermarkOverlay(gpsData, settings, finalMapBitmap, recordingStartTime, azimuth, videoWidth, videoHeight)
                    val overlayEffect = OverlayEffect(listOf(overlay) as List<androidx.media3.effect.TextureOverlay>)
                    val effects = Effects(
                        listOf<AudioProcessor>(),
                        listOf<Effect>(overlayEffect)
                    )
                    
                    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                        .setEffects(effects)
                        .build()
                        
                    val encoderFactory = androidx.media3.transformer.DefaultEncoderFactory.Builder(context)
                        .setEnableFallback(true)
                        .build()

                    val transformer = Transformer.Builder(context)
                        .setEncoderFactory(encoderFactory)
                        .build()
                        
                    transformer.addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            Log.d(TAG, "Video watermarking transcode completed successfully!")
                            // Save to gallery off the main thread
                            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                                val finalUri = saveVideoToGallery(context, outputTranscodedFile, displayName, gpsData)
                                onComplete(finalUri)
                                
                                try {
                                    tempFile.delete()
                                    outputTranscodedFile.delete()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to clean up temp files", e)
                                }
                            }
                        }
                        
                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            Log.e(TAG, "Video watermarking transcode failed! Fallback to saving original.", exportException)
                            java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                                val finalUri = saveVideoToGallery(context, tempFile, displayName, gpsData)
                                onComplete(finalUri)
                                
                                try {
                                    tempFile.delete()
                                    outputTranscodedFile.delete()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to clean up temp files", e)
                                }
                            }
                        }
                    })
                    
                    transformer.start(editedMediaItem, outputTranscodedFile.absolutePath)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error starting transcode inside Handler, saving original", e2)
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute {
                        val finalUri = saveVideoToGallery(context, tempFile, displayName, gpsData)
                        onComplete(finalUri)
                        try { tempFile.delete() } catch (ex: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting video transcode, saving original", e)
            val finalUri = saveVideoToGallery(context, tempFile, displayName, gpsData)
            onComplete(finalUri)
            try { tempFile.delete() } catch (ex: Exception) {}
        }
    }

    private fun saveVideoToGallery(
        context: Context,
        videoFile: File,
        displayName: String,
        gpsData: GPSData
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$ALBUM_NAME")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        var videoUri: Uri? = null
        try {
            videoUri = resolver.insert(collectionUri, contentValues)
            if (videoUri != null) {
                resolver.openOutputStream(videoUri).use { outputStream ->
                    if (outputStream != null) {
                        FileInputStream(videoFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(videoUri, contentValues, null, null)
                }
                
                scanUri(context, videoUri)
                Log.d(TAG, "Video saved successfully to Gallery: $videoUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving video to Gallery", e)
        }

        saveCompanionMetadata(context, displayName, gpsData)
        return videoUri
    }

    private fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        settings: SettingsData
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        // Quality compression percentage: Low = 55%, Medium = 75%, High = 95%
        val compressQuality = when (settings.imageQuality) {
            "Low" -> 55
            "Medium" -> 75
            else -> 95
        }

        // Downscale image dimensions dynamically to make photo saving blazingly fast and extremely light (small MBs)
        val finalBitmap = if (settings.imageQuality == "Low") {
            val targetWidth = 1280
            if (bitmap.width > targetWidth) {
                val factor = targetWidth.toFloat() / bitmap.width
                val targetHeight = (bitmap.height * factor).toInt()
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }
        } else if (settings.imageQuality == "Medium") {
            val targetWidth = 1920
            if (bitmap.width > targetWidth) {
                val factor = targetWidth.toFloat() / bitmap.width
                val targetHeight = (bitmap.height * factor).toInt()
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }
        } else {
            bitmap
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collectionUri, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, outputStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                scanUri(context, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to MediaStore", e)
        } finally {
            if (finalBitmap !== bitmap) {
                finalBitmap.recycle()
            }
        }
        return uri
    }

    private fun scanUri(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = uri
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast media scan intent", e)
        }
    }

    private fun saveCompanionMetadata(
        context: Context,
        mediaName: String,
        gpsData: GPSData
    ) {
        val metadataName = mediaName.substringBeforeLast(".") + "_metadata.json"
        val metadataJson = """
            {
              "mediaName": "$mediaName",
              "latitude": ${gpsData.latitude},
              "longitude": ${gpsData.longitude},
              "altitude": ${gpsData.altitude},
              "speed": ${gpsData.speed},
              "address": "${gpsData.address.replace("\"", "\\\"")}",
              "city": "${gpsData.city}",
              "country": "${gpsData.country}",
              "date": "${gpsData.formattedDate}",
              "time": "${gpsData.formattedTime}",
              "timezone": "${gpsData.timezone}"
            }
        """.trimIndent()

        try {
            val rootFolder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (rootFolder != null) {
                val albumFolder = File(rootFolder, ALBUM_NAME)
                if (!albumFolder.exists()) albumFolder.mkdirs()
                val metadataFile = File(albumFolder, metadataName)
                FileOutputStream(metadataFile).use { fos ->
                    fos.write(metadataJson.toByteArray())
                }
                Log.d(TAG, "Companion metadata saved at: ${metadataFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save companion metadata", e)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoWatermarkOverlay(
    private val gpsData: GPSData,
    private val settings: SettingsData,
    private val mapBitmap: Bitmap?,
    private val recordingStartTime: Long,
    private val azimuth: Float,
    private val videoWidth: Int,
    private val videoHeight: Int
) : BitmapOverlay() {
    
    private var lastSecond = -1L
    private var reusableBitmap: Bitmap? = null
    private var canvas: Canvas? = null

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val seconds = presentationTimeUs / 1_000_000
        if (seconds == lastSecond && reusableBitmap != null) {
            return reusableBitmap!!
        }
        lastSecond = seconds

        val bitmap = reusableBitmap ?: Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888).also {
            reusableBitmap = it
            canvas = Canvas(it)
        }
        
        // Reset/clear background to transparent for the new frame draw
        bitmap.eraseColor(android.graphics.Color.TRANSPARENT)

        val frameTimeMs = recordingStartTime + (presentationTimeUs / 1000)
        
        WatermarkGenerator.drawWatermarkOnCanvas(
            canvas = canvas!!,
            gpsData = gpsData,
            settings = settings,
            mapBitmap = mapBitmap,
            width = videoWidth.toFloat(),
            height = videoHeight.toFloat(),
            azimuth = azimuth,
            dynamicTimeMs = frameTimeMs
        )

        return bitmap
    }
}
