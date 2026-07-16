package com.example.ui.components

import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SettingsData
import com.example.model.WatermarkTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    settings: SettingsData,
    onSettingsChanged: (SettingsData) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E12))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // HEADER ROW
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Camera Settings",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.2f),
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // SECTION 1: WATERMARK TEMPLATES
        item {
            SettingsSectionHeader(title = "Watermark Design Templates")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WatermarkTemplate.values()) { t ->
                    val isSelected = settings.template == t
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Color(0xFF00E676) else Color.White.copy(
                                    alpha = 0.05f
                                )
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSettingsChanged(settings.copy(template = t))
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayName = when (t) {
                            WatermarkTemplate.CLASSIC -> "Classic Template"
                            WatermarkTemplate.PROFESSIONAL -> "Scan Location Template"
                            WatermarkTemplate.SIMPLE -> "Minimal Template"
                            WatermarkTemplate.SURVEY -> "Government Survey Template"
                            WatermarkTemplate.FIELD_INSPECTION -> "Reporting Template"
                            WatermarkTemplate.COMPASS -> "Navigation Compass Template"
                            WatermarkTemplate.CORPORATE -> "DateTime Template"
                            WatermarkTemplate.DEFAULT -> "Advance Template"
                            WatermarkTemplate.GPS_CAMERA_STYLE -> "GPS Camera Style"
                            WatermarkTemplate.GPS_CAMERA_NO_LOGO -> "GPS Camera (No Logo)"
                            else -> t.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } + " Template"
                        }
                        Text(
                            text = displayName,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION 2: LOCATION TELEMETRY & MANUAL LOCATION
        item {
            SettingsSectionHeader(title = "Location Telemetry")
            
            // Manual Location Switch Row
            SettingsSwitchRow(
                icon = Icons.Default.Map,
                title = "Use Manual Location",
                description = "Override dynamic live GPS coordinates with custom address pin.",
                checked = settings.useManualLocation,
                onCheckedChange = {
                    onSettingsChanged(settings.copy(useManualLocation = it))
                }
            )

            AnimatedVisibility(visible = settings.useManualLocation) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Search Address or Place",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("e.g. San Francisco, CA") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (searchQuery.isBlank()) return@Button
                                isSearching = true
                                searchError = null
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        @Suppress("DEPRECATION")
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = geocoder.getFromLocationName(searchQuery, 1)
                                        if (!addresses.isNullOrEmpty()) {
                                            val addr = addresses[0]
                                            val fullAddress = addr.getAddressLine(0) ?: searchQuery
                                            val city = addr.locality ?: addr.subLocality ?: addr.subAdminArea ?: ""
                                            val country = addr.countryName ?: ""
                                            withContext(Dispatchers.Main) {
                                                onSettingsChanged(
                                                    settings.copy(
                                                        manualLatitude = addr.latitude,
                                                        manualLongitude = addr.longitude,
                                                        manualAddress = fullAddress,
                                                        manualCity = city,
                                                        manualCountry = country
                                                    )
                                                )
                                                isSearching = false
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                searchError = "No address found for query."
                                                isSearching = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            searchError = "Geocoding failed. Try coordinates."
                                            isSearching = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
                            }
                        }
                    }

                    searchError?.let { err ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = err, color = Color.Red, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Editable Coordinates Fields
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = settings.manualLatitude.toString(),
                            onValueChange = {
                                val value = it.toDoubleOrNull() ?: return@OutlinedTextField
                                onSettingsChanged(settings.copy(manualLatitude = value))
                            },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = settings.manualLongitude.toString(),
                            onValueChange = {
                                val value = it.toDoubleOrNull() ?: return@OutlinedTextField
                                onSettingsChanged(settings.copy(manualLongitude = value))
                            },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Editable Address Row
                    OutlinedTextField(
                        value = settings.manualAddress,
                        onValueChange = {
                            onSettingsChanged(settings.copy(manualAddress = it))
                        },
                        label = { Text("Custom Address Text") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION 3: DATE & TIME FORMATS
        item {
            SettingsSectionHeader(title = "Formats & Representation")

            // Date Format row
            SettingsSelectionRow(
                title = "Date Format",
                options = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "EEEE, dd MMMM yyyy"),
                selected = settings.dateFormat,
                onSelected = { onSettingsChanged(settings.copy(dateFormat = it)) }
            )

            // Time Format row
            SettingsSelectionRow(
                title = "Time Format",
                options = listOf("hh:mm a", "hh:mm:ss a", "HH:mm", "HH:mm:ss"),
                selected = settings.timeFormat,
                onSelected = { onSettingsChanged(settings.copy(timeFormat = it)) }
            )

            // Coordinate Format row
            SettingsSelectionRow(
                title = "Coordinate Format",
                options = listOf("DD", "DMS"),
                selected = settings.coordinateFormat,
                onSelected = { onSettingsChanged(settings.copy(coordinateFormat = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION 4: WATERMARK LAYOUT CONTROLS
        item {
            SettingsSectionHeader(title = "Watermark Customization")

            SettingsSelectionRow(
                title = "Position",
                options = listOf("Bottom", "Top"),
                selected = settings.watermarkPosition,
                onSelected = { onSettingsChanged(settings.copy(watermarkPosition = it)) }
            )

            SettingsSelectionRow(
                title = "Size",
                options = listOf("Small", "Medium", "Large"),
                selected = settings.watermarkSize,
                onSelected = { onSettingsChanged(settings.copy(watermarkSize = it)) }
            )

            SettingsSelectionRow(
                title = "Map View Style",
                options = listOf("Satellite", "Road", "Hybrid"),
                selected = settings.mapViewType,
                onSelected = { onSettingsChanged(settings.copy(mapViewType = it)) }
            )

            SettingsSelectionRow(
                title = "Image Quality",
                options = listOf("Low", "Medium", "High"),
                selected = settings.imageQuality,
                onSelected = { onSettingsChanged(settings.copy(imageQuality = it)) }
            )

            // Card Opacity slider
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Card Opacity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${(settings.opacity * 100).toInt()}%", color = Color(0xFF00E676), fontSize = 14.sp)
                }
                Slider(
                    value = settings.opacity,
                    onValueChange = { onSettingsChanged(settings.copy(opacity = it)) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E676),
                        activeTrackColor = Color(0xFF00E676)
                    )
                )
            }

            // Flags, Minimap, Compass, Rounded, Blur switches
            SettingsSwitchRow(
                icon = Icons.Default.Flag,
                title = "Show Country Flag",
                description = "Embed national emoji flag on watermark title line.",
                checked = settings.showCountryFlag,
                onCheckedChange = { onSettingsChanged(settings.copy(showCountryFlag = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.Map,
                title = "Show Mini Map",
                description = "Display rounded high resolution Google satellite thumbnail.",
                checked = settings.showMiniMap,
                onCheckedChange = { onSettingsChanged(settings.copy(showMiniMap = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.Explore,
                title = "Show Compass",
                description = "Embed dynamic rotating physical compass ring.",
                checked = settings.showCompass,
                onCheckedChange = { onSettingsChanged(settings.copy(showCompass = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.RoundedCorner,
                title = "Rounded Corners",
                description = "Apply Material 3 sleek curved container layout shape.",
                checked = settings.roundedCorners,
                onCheckedChange = { onSettingsChanged(settings.copy(roundedCorners = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.BlurOn,
                title = "Glass Blur Effect",
                description = "Add premium glassmorphism translucent background layer.",
                checked = settings.glassBlur,
                onCheckedChange = { onSettingsChanged(settings.copy(glassBlur = it)) }
            )

            SettingsSwitchRow(
                icon = Icons.Default.VolumeUp,
                title = "Camera Shutter Sound",
                description = "Play professional haptic feedback and shutter sound cues.",
                checked = settings.shutterSound,
                onCheckedChange = { onSettingsChanged(settings.copy(shutterSound = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION 5: GENERAL PREFERENCES
        item {
            SettingsSectionHeader(title = "General Preferences")

            SettingsSelectionRow(
                title = "App Theme",
                options = listOf("Dark", "Light", "System"),
                selected = settings.theme,
                onSelected = { onSettingsChanged(settings.copy(theme = it)) }
            )

            SettingsSelectionRow(
                title = "Language",
                options = listOf("en", "es", "fr", "hi", "ja"),
                selected = settings.language,
                onSelected = { onSettingsChanged(settings.copy(language = it)) }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF00E676),
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = description, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF00E676)
            )
        )
    }
}

@Composable
fun SettingsSelectionRow(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.05f))
                        .clickable { onSelected(option) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
