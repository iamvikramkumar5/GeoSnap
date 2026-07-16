package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryView(
    mediaFiles: List<File>,
    onClose: () -> Unit,
    onDeleteFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<File?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E12)) // Deep slate dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GEOSNAP VAULT",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${mediaFiles.size} Items Available",
                        color = Color(0xFF00E676), // Neon green accent
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Vault",
                        tint = Color.White
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

            if (mediaFiles.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No captured media found",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Photos and videos captured with GeoSnap will appear here instantly.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Media Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(4.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(mediaFiles) { file ->
                        val isVideo = file.extension.equals("mp4", true)
                        val isOriginal = file.name.contains("_original")

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E232B))
                                .border(
                                    1.dp,
                                    if (isOriginal) Color.White.copy(alpha = 0.1f) else Color(0xFF00E676).copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFile = file },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Media preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Video overlay badge
                            if (isVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Original VS Watermark tag
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        if (isOriginal) Color.DarkGray.copy(alpha = 0.85f) else Color(0xFF00E676).copy(alpha = 0.85f),
                                        RoundedCornerShape(topStart = 8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isOriginal) "RAW" else "SNAP",
                                    color = if (isOriginal) Color.White else Color.Black,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded full-screen viewer overlay
        selectedFile?.let { file ->
            FullscreenMediaViewer(
                file = file,
                onClose = { selectedFile = null },
                onDelete = {
                    onDeleteFile(file)
                    selectedFile = null
                }
            )
        }
    }
}

@Composable
fun FullscreenMediaViewer(
    file: File,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = file.extension.equals("mp4", true)
    var metadata by remember { mutableStateOf<Map<String, String>?>(null) }

    // Load companion metadata on file change
    LaunchedEffect(file) {
        val rawJson = loadMetadataJson(context, file)
        if (rawJson != null) {
            metadata = parseMetadataJson(rawJson)
        } else {
            metadata = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Viewer (Image or Video)
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                VideoPlayer(file = file)
            }
        } else {
            AsyncImage(
                model = file,
                contentDescription = "Expanded image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Header Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share
                IconButton(
                    onClick = { shareFile(context, file) },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        }

        // Bottom Telemetry Overlay Drawer (Glassmorphic)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xE610141C))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = if (isVideo) "VIDEO METADATA" else "PHOTO TELEMETRY",
                color = Color(0xFF00E676),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            metadata?.let { data ->
                Text(
                    text = data["address"] ?: "Location Coordinates Secured",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "LATITUDE", color = Color.Gray, fontSize = 9.sp)
                        Text(text = data["latitude"] ?: "0.0", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "LONGITUDE", color = Color.Gray, fontSize = 9.sp)
                        Text(text = data["longitude"] ?: "0.0", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ALTITUDE", color = Color.Gray, fontSize = 9.sp)
                        Text(text = data["altitude"] ?: "0m", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SPEED", color = Color.Gray, fontSize = 9.sp)
                        Text(text = data["speed"] ?: "0.0", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(text = "TIMESTAMP", color = Color.Gray, fontSize = 9.sp)
                        Text(text = "${data["date"]} @ ${data["time"]}", color = Color.White, fontSize = 11.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ZONE", color = Color.Gray, fontSize = 9.sp)
                        Text(text = data["timezone"]?.substringBefore(" ") ?: "UTC", color = Color.White, fontSize = 11.sp)
                    }
                }
            } ?: run {
                // Fallback to simple file metadata
                Text(
                    text = file.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Size: ${String.format("%.2f MB", file.length() / (1024f * 1024f))} | Album: GeoSnap",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun VideoPlayer(file: File, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setVideoPath(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

// Helper methods to read companion JSON files and parse them manually
private fun loadMetadataJson(context: Context, mediaFile: File): String? {
    return try {
        val nameWithoutExt = mediaFile.nameWithoutExtension
        val metadataName = "${nameWithoutExt}_metadata.json"
        
        val rootFolder = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val albumFolder = File(rootFolder, "GeoSnap")
        val metadataFile = File(albumFolder, metadataName)
        
        if (metadataFile.exists()) {
            metadataFile.readText()
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("GalleryView", "Failed to load companion JSON", e)
        null
    }
}

private fun parseMetadataJson(jsonStr: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    try {
        // Pure regex manual fast parser
        val regex = "\"(\\w+)\"\\s*:\\s*\"?([^\",}]+)\"?".toRegex()
        val matches = regex.findAll(jsonStr)
        for (match in matches) {
            val key = match.groupValues[1].trim()
            val value = match.groupValues[2].trim().replace("\\\"", "\"")
            map[key] = value
        }
    } catch (e: Exception) {
        Log.e("GalleryView", "Manual JSON parse exception", e)
    }
    return map
}

private fun shareFile(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension.equals("mp4", true)) "video/mp4" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media with GPS Info"))
    } catch (e: Exception) {
        Log.e("GalleryView", "Failed to share media file", e)
    }
}
