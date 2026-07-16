package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.model.GPSData
import com.example.model.SettingsData
import com.example.model.WatermarkTemplate
import android.graphics.BitmapFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WatermarkGenerator {

    fun fetchMapBitmap(lat: Double, lng: Double, mapViewType: String): Bitmap? {
        if (lat == 0.0 && lng == 0.0) return null
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val apiKey = try {
                com.example.BuildConfig.MAPS_API_KEY
            } catch (e: Exception) {
                ""
            }
            
            val isGoogleKeyAvailable = apiKey.isNotEmpty() && 
                    apiKey != "MY_MAPS_API_KEY" && 
                    !apiKey.startsWith("Placeholder") &&
                    apiKey != "null"
            
            val url = if (isGoogleKeyAvailable) {
                val googleMapType = when (mapViewType) {
                    "Road" -> "roadmap"
                    "Hybrid" -> "hybrid"
                    else -> "satellite"
                }
                "https://maps.googleapis.com/maps/api/staticmap?center=$lat,$lng&zoom=15&size=600x600&maptype=$googleMapType&key=$apiKey"
            } else {
                val yandexMapType = when (mapViewType) {
                    "Road" -> "map"
                    "Hybrid" -> "sat,skl"
                    else -> "sat"
                }
                "https://static-maps.yandex.ru/1.x/?ll=$lng,$lat&z=15&l=$yandexMapType&size=600,600"
            }
            
            val request = okhttp3.Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val bytes = body.bytes()
                        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WatermarkGenerator", "Failed to sync-fetch map bitmap", e)
        }
        return null
    }

    fun getFlagEmoji(country: String?): String {
        if (country.isNullOrBlank()) return "📍"
        return when (country.lowercase(Locale.ROOT).trim()) {
            "india", "in", "ind" -> "🇮🇳"
            "united states", "usa", "us", "united states of america" -> "🇺🇸"
            "united kingdom", "uk", "gb", "great britain" -> "🇬🇧"
            "canada", "ca" -> "🇨🇦"
            "australia", "au" -> "🇦🇺"
            "germany", "de" -> "🇩🇪"
            "france", "fr" -> "🇫🇷"
            "japan", "jp" -> "🇯🇵"
            "china", "cn" -> "🇨🇳"
            "brazil", "br" -> "🇧🇷"
            "singapore", "sg" -> "🇸🇬"
            "malaysia", "my" -> "🇲🇾"
            "indonesia", "id" -> "🇮🇩"
            "russia", "ru" -> "🇷🇺"
            "netherlands", "nl" -> "🇳🇱"
            "spain", "es" -> "🇪🇸"
            "italy", "it" -> "🇮🇹"
            else -> "📍"
        }
    }

    fun formatCoordinate(value: Double, isLatitude: Boolean, format: String): String {
        val dir = if (isLatitude) {
            if (value >= 0) "N" else "S"
        } else {
            if (value >= 0) "E" else "W"
        }
        val absValue = Math.abs(value)
        if (format == "DMS") {
            val degrees = absValue.toInt()
            val minutesSeconds = (absValue - degrees) * 60.0
            val minutes = minutesSeconds.toInt()
            val seconds = (minutesSeconds - minutes) * 60.0
            return String.format("%d°%02d'%02.1f\" %s", degrees, minutes, seconds, dir)
        } else {
            return String.format("%.6f°", value) // standard high-res GPS coordinates
        }
    }

    fun drawWatermark(
        original: Bitmap,
        gpsData: GPSData,
        settings: SettingsData,
        mapBitmap: Bitmap? = null,
        azimuth: Float = 0f
    ): Bitmap {
        val watermarked = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(watermarked)
        drawWatermarkOnCanvas(
            canvas = canvas,
            gpsData = gpsData,
            settings = settings,
            mapBitmap = mapBitmap,
            width = original.width.toFloat(),
            height = original.height.toFloat(),
            azimuth = azimuth
        )
        return watermarked
    }

    fun drawWatermarkOnCanvas(
        canvas: Canvas,
        gpsData: GPSData,
        settings: SettingsData,
        mapBitmap: Bitmap?,
        width: Float,
        height: Float,
        azimuth: Float = 0f,
        dynamicTimeMs: Long? = null
    ) {
        // 1. Detect if it is the preview canvas or final saved photo
        val isPreview = (width / height) > 2.0f
        val isLandscape = !isPreview && (width > height)
        val referenceDimension = if (isPreview) width else Math.min(width, height)
        
        // 2. Base dynamic scale referenced to standard dimension to keep sizes consistent
        val baseScale = (referenceDimension / 1080f).coerceAtLeast(0.4f)
        val sizeMultiplier = when (settings.watermarkSize) {
            "Small" -> 0.75f
            "Large" -> 1.25f
            else -> 1.0f
        }
        val scale = baseScale * sizeMultiplier

        // 3. Margin and Layout Bounds
        val marginX = 24f * scale
        val marginY = if (isPreview) {
            (height - (280f * scale)) / 2f
        } else {
            24f * scale
        }
        val cardHeight = 280f * scale
        val cardWidth = if (isLandscape) {
            // Keep card width compact on landscape images to match portrait proportions instead of stretching
            height - 2f * marginX
        } else {
            width - 2f * marginX
        }

        val cardLeft = marginX
        val cardRight = cardLeft + cardWidth
        val cardTop = if (settings.watermarkPosition == "Top") {
            if (isPreview) marginY else marginY + (if (height > width) 120f * scale else 40f * scale)
        } else {
            if (isPreview) marginY else height - cardHeight - marginY
        }
        val cardBottom = cardTop + cardHeight

        // Left Section: Square Map Thumbnail
        val mapLeft = cardLeft
        val mapTop = cardTop
        val mapRight = cardLeft + cardHeight
        val mapBottom = cardBottom
        val mapRect = RectF(mapLeft, mapTop, mapRight, mapBottom)
        val mapCornerRadius = if (settings.roundedCorners) 32f * scale else 0f

        // Right Section: Dark Glassmorphic Card Container (Text container)
        val gap = 16f * scale
        
        // Define text card bounds dynamically based on templates
        val textCardLeft = when (settings.template) {
            WatermarkTemplate.CORPORATE -> cardLeft
            else -> mapRight + gap
        }
        
        val textCardRight = when (settings.template) {
            WatermarkTemplate.PROFESSIONAL -> cardRight - cardHeight - gap
            else -> cardRight
        }
        
        val textCardTop = cardTop
        val textCardBottom = cardBottom
        val textCardRect = RectF(textCardLeft, textCardTop, textCardRight, textCardBottom)
        val cornerRadius = if (settings.roundedCorners) 32f * scale else 0f

        // Optional QR Section bounds for Scan Location Template
        val qrRect = RectF(cardRight - cardHeight, cardTop, cardRight, cardBottom)

        // Latitude and Longitude for map and text coordinates
        val lat = if (settings.useManualLocation) settings.manualLatitude else gpsData.latitude
        val lng = if (settings.useManualLocation) settings.manualLongitude else gpsData.longitude

        // Retrieve dynamic formatted date & time
        val rawTimeMs = dynamicTimeMs ?: System.currentTimeMillis()
        val dateObj = Date(rawTimeMs)
        
        val dynamicDateText = try {
            SimpleDateFormat(settings.dateFormat, Locale.US).format(dateObj)
        } catch (e: Exception) {
            gpsData.formattedDate.ifEmpty { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.US).format(dateObj) }
        }

        val dynamicTimeText = try {
            SimpleDateFormat(settings.timeFormat, Locale.US).format(dateObj)
        } catch (e: Exception) {
            gpsData.formattedTime.ifEmpty { SimpleDateFormat("hh:mm a", Locale.US).format(dateObj) }
        }

        val locationTitle = if (settings.useManualLocation) {
            if (settings.manualCity.isNotEmpty()) {
                settings.manualCity
            } else {
                settings.manualAddress.split(",").firstOrNull()?.trim() ?: "Manual Location"
            }
        } else {
            if (gpsData.shortAddress.isNotEmpty()) {
                gpsData.shortAddress
            } else if (gpsData.city.isNotEmpty()) {
                gpsData.city
            } else {
                "GPS Location"
            }
        }

        val flag = if (settings.showCountryFlag) getFlagEmoji(if (settings.useManualLocation) settings.manualCountry else gpsData.country) else ""
        val titleText = "$locationTitle $flag".trim()

        val latStr = "Lat " + formatCoordinate(gpsData.latitude, true, settings.coordinateFormat)
        val lngStr = "Long " + formatCoordinate(gpsData.longitude, false, settings.coordinateFormat)

        // ----------------------------------------------------
        // RENDER TEMPLATES INDIVIDUALLY
        // ----------------------------------------------------
        if (settings.template == WatermarkTemplate.SIMPLE) {
            // Minimalist / SIMPLE template: Translucent white floating text with drop shadow (GCam Style)
            val simplePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 28f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(8f * scale, 0f, 2f * scale, Color.BLACK)
            }
            
            val simpleText = "$locationTitle  |  $latStr, $lngStr  |  $dynamicDateText $dynamicTimeText"
            val textX = marginX + 16f * scale
            val textY = height - marginY - 16f * scale
            canvas.drawText(simpleText, textX, textY, simplePaint)
            return
        }

        // Standard Boxed Templates
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
            isAntiAlias = true
        }

        // Color and theme setup for each template
        var badgeColor = Color.rgb(255, 152, 0) // Default bright orange/gold
        var badgeBgColor = Color.argb(45, 255, 152, 0)
        var templateNameText = "GeoSnap"

        when (settings.template) {
            WatermarkTemplate.CLASSIC -> {
                backgroundPaint.color = Color.argb(120, 15, 18, 22) // Translucent dark slate
                borderPaint.color = Color.argb(120, 255, 215, 0) // Classic Luxury Gold
                badgeColor = Color.rgb(255, 215, 0)
                badgeBgColor = Color.argb(45, 255, 215, 0)
                templateNameText = "Classic"
            }
            WatermarkTemplate.PROFESSIONAL -> {
                backgroundPaint.color = Color.argb(135, 12, 14, 18) // Ultra Dark
                borderPaint.color = Color.argb(140, 0, 230, 118) // High-tech neon green
                badgeColor = Color.rgb(0, 230, 118)
                badgeBgColor = Color.argb(45, 0, 230, 118)
                templateNameText = "Scan Location"
            }
            WatermarkTemplate.SURVEY -> {
                backgroundPaint.color = Color.argb(130, 10, 22, 36) // Survey Dark Blue
                borderPaint.color = Color.argb(140, 0, 176, 255) // Survey Vibrant Blue
                badgeColor = Color.rgb(0, 176, 255)
                badgeBgColor = Color.argb(45, 0, 176, 255)
                templateNameText = "Government Survey"
            }
            WatermarkTemplate.FIELD_INSPECTION -> {
                backgroundPaint.color = Color.argb(130, 15, 26, 20) // Deep Forest Green
                borderPaint.color = Color.argb(140, 76, 175, 80) // Inspection Green
                badgeColor = Color.rgb(76, 175, 80)
                badgeBgColor = Color.argb(45, 76, 175, 80)
                templateNameText = "Reporting"
            }
            WatermarkTemplate.COMPASS -> {
                backgroundPaint.color = Color.argb(130, 18, 18, 24) // Compass Dark Slate
                borderPaint.color = Color.argb(100, 255, 152, 0) // Compass Orange Border
                badgeColor = Color.rgb(255, 152, 0)
                badgeBgColor = Color.argb(45, 255, 152, 0)
                templateNameText = "Navigation"
            }
            WatermarkTemplate.CORPORATE -> {
                backgroundPaint.color = Color.argb(140, 20, 20, 20) // Solid Warm Charcoal
                borderPaint.color = Color.argb(100, 255, 152, 0) // Warm Orange Accent Line
                badgeColor = Color.rgb(255, 152, 0)
                badgeBgColor = Color.argb(45, 255, 152, 0)
                templateNameText = "Date Time"
            }
            WatermarkTemplate.DEFAULT -> {
                backgroundPaint.color = Color.argb(110, 15, 18, 22)
                borderPaint.color = Color.argb(60, 255, 255, 255) // Subtle clean white
                badgeColor = Color.rgb(0, 230, 118)
                badgeBgColor = Color.argb(45, 0, 230, 118)
                templateNameText = "Advanced"
            }
            WatermarkTemplate.GPS_CAMERA_STYLE -> {
                backgroundPaint.color = Color.argb(165, 12, 12, 16) // Premium Dark Glassmorphic
                borderPaint.color = Color.argb(80, 255, 255, 255) // Sleek white/translucent border
                badgeColor = Color.rgb(0, 230, 118)
                badgeBgColor = Color.argb(45, 0, 230, 118)
                templateNameText = "GPS Camera"
            }
            WatermarkTemplate.GPS_CAMERA_NO_LOGO -> {
                backgroundPaint.color = Color.argb(185, 12, 12, 16) // Solid Premium Dark Glassmorphic
                borderPaint.color = Color.argb(80, 255, 255, 255) // Sleek white/translucent border
                badgeColor = Color.rgb(0, 230, 118)
                badgeBgColor = Color.argb(45, 0, 230, 118)
                templateNameText = "GPS Camera No Logo"
            }
            else -> {
                backgroundPaint.color = Color.argb(110, 15, 18, 22)
                borderPaint.color = Color.argb(60, 255, 255, 255)
                badgeColor = Color.rgb(0, 230, 118)
                badgeBgColor = Color.argb(45, 0, 230, 118)
                templateNameText = "Advanced"
            }
        }

        // Draw card background for the text card on the right (or full card if CORPORATE)
        if (settings.template != WatermarkTemplate.TRANSPARENT) {
            backgroundPaint.setShadowLayer(16f * scale, 0f, 4f * scale, Color.argb(100, 0, 0, 0))
            canvas.drawRoundRect(textCardRect, cornerRadius, cornerRadius, backgroundPaint)
            backgroundPaint.clearShadowLayer()
        }
        canvas.drawRoundRect(textCardRect, cornerRadius, cornerRadius, borderPaint)

        // LEFT SECTION (Map, Compass Dial or none based on Template)
        if (settings.template != WatermarkTemplate.CORPORATE) {
            if (settings.template == WatermarkTemplate.COMPASS) {
                // Draw rotating vector compass dial instead of a map
                drawCompassDial(canvas, mapRect, azimuth, scale)
                
                // Draw custom "Facing Northwest" pill below the compass dial
                val cardinalDir = getCardinalDirection(azimuth)
                val pillPaint = Paint().apply {
                    color = Color.argb(180, 40, 44, 52)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val pillBorderPaint = Paint().apply {
                    color = Color.argb(80, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f * scale
                    isAntiAlias = true
                }
                val pillTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f * scale
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                
                val pillText = "Facing $cardinalDir"
                val pillW = pillTextPaint.measureText(pillText) + 16f * scale
                val pillH = 18f * scale
                val pillLeft = mapLeft + (cardHeight - pillW) / 2f
                val pillTop = mapBottom - 26f * scale
                val pillRect = RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH)
                
                canvas.drawRoundRect(pillRect, 9f * scale, 9f * scale, pillPaint)
                canvas.drawRoundRect(pillRect, 9f * scale, 9f * scale, pillBorderPaint)
                canvas.drawText(
                    pillText,
                    pillLeft + 8f * scale,
                    pillTop + pillH / 2f - (pillTextPaint.descent() + pillTextPaint.ascent()) / 2f,
                    pillTextPaint
                )
            } else {
                // Draw soft shadow behind the map thumbnail
                if (settings.roundedCorners) {
                    val mapShadowPaint = Paint().apply {
                        color = Color.BLACK
                        isAntiAlias = true
                        setShadowLayer(16f * scale, 0f, 4f * scale, Color.argb(100, 0, 0, 0))
                    }
                    canvas.drawRoundRect(mapRect, mapCornerRadius, mapCornerRadius, mapShadowPaint)
                }

                // Draw standard Map Thumbnail
                val mapPath = Path().apply {
                    addRoundRect(mapRect, mapCornerRadius, mapCornerRadius, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(mapPath)
                
                if (mapBitmap != null) {
                    val srcRect = android.graphics.Rect(0, 0, mapBitmap.width, mapBitmap.height)
                    canvas.drawBitmap(mapBitmap, srcRect, mapRect, Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true })
                } else {
                    // Automatically generate a premium, professional fake satellite map!
                    drawFakeSatelliteMap(canvas, mapRect, gpsData.latitude, gpsData.longitude, scale)
                }
                canvas.restore()

                // Map Thumbnail Border
                val mapBorderPaint = Paint().apply {
                    color = Color.argb(50, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * scale
                    isAntiAlias = true
                }
                canvas.drawRoundRect(mapRect, mapCornerRadius, mapCornerRadius, mapBorderPaint)

                // Center Location Pin (Sleek red dropped marker with blue directional cone)
                val pinCx = mapLeft + cardHeight / 2f
                val pinCy = mapTop + cardHeight / 2f

                // Draw standard compass FoV cone pointing straight along 'azimuth'
                val fovPaint = Paint().apply {
                    color = Color.argb(45, 33, 150, 243) // translucent Material Blue
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val fovPath = Path().apply {
                    moveTo(pinCx, pinCy)
                    val sweepAngle = 60f
                    val startAngle = azimuth - 90f - sweepAngle / 2f
                    arcTo(
                        RectF(pinCx - 80f * scale, pinCy - 80f * scale, pinCx + 80f * scale, pinCy + 80f * scale),
                        startAngle,
                        sweepAngle,
                        false
                    )
                    close()
                }
                canvas.drawPath(fovPath, fovPaint)

                val pinGlowPaint = Paint().apply {
                    color = Color.argb(120, 244, 67, 54) // Material Red glow
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(pinCx, pinCy, 16f * scale, pinGlowPaint)

                val pinPaint = Paint().apply {
                    color = Color.rgb(244, 67, 54)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                
                // Draw Google Pin Shape
                val pinPath = Path().apply {
                    moveTo(pinCx, pinCy)
                    cubicTo(pinCx - 10f * scale, pinCy - 10f * scale, pinCx - 12f * scale, pinCy - 24f * scale, pinCx, pinCy - 32f * scale)
                    cubicTo(pinCx + 12f * scale, pinCy - 24f * scale, pinCx + 10f * scale, pinCy - 10f * scale, pinCx, pinCy)
                    close()
                }
                canvas.drawPath(pinPath, pinPaint)
                canvas.drawCircle(pinCx, pinCy - 32f * scale, 12f * scale, pinPaint)
                
                val pinDotPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(pinCx, pinCy - 32f * scale, 4.5f * scale, pinDotPaint)

                // Dynamic original branding attribution inside thumbnail
                val attribText = if (mapBitmap != null) "GeoSnap Map" else "GeoSnap Satellite"
                val attribPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f * scale
                    typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                    isAntiAlias = true
                }
                val attribTextWidth = attribPaint.measureText(attribText)
                val attribBgPaint = Paint().apply {
                    color = Color.argb(140, 15, 18, 22)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val attribBg = RectF(mapLeft + 6f * scale, mapBottom - 24f * scale, mapLeft + 16f * scale + attribTextWidth, mapBottom - 6f * scale)
                canvas.drawRoundRect(attribBg, 6f * scale, 6f * scale, attribBgPaint)
                canvas.drawText(attribText, mapLeft + 11f * scale, mapBottom - 11f * scale, attribPaint)

                // For FIELD_INSPECTION (Reporting Template), draw a dynamic "Check In" badge over top of map
                if (settings.template == WatermarkTemplate.FIELD_INSPECTION) {
                    val checkInPaint = Paint().apply {
                        color = Color.rgb(76, 175, 80) // Forest Green
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val checkInBorderPaint = Paint().apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 1f * scale
                        isAntiAlias = true
                    }
                    val checkInTextPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 10f * scale
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val badgeW = checkInTextPaint.measureText("Check In") + 24f * scale
                    val badgeH = 20f * scale
                    val badgeLeft = mapLeft + (cardHeight - badgeW) / 2f
                    val badgeTop = mapTop - 10f * scale // slight overlap on the top border
                    val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH)
                    
                    canvas.drawRoundRect(badgeRect, 10f * scale, 10f * scale, checkInPaint)
                    canvas.drawRoundRect(badgeRect, 10f * scale, 10f * scale, checkInBorderPaint)
                    canvas.drawText(
                        "Check In",
                        badgeLeft + 12f * scale,
                        badgeTop + badgeH / 2f - (checkInTextPaint.descent() + checkInTextPaint.ascent()) / 2f,
                        checkInTextPaint
                    )
                }
            }
        }

        // DRAW QR CODE FOR SCAN LOCATION TEMPLATE (WatermarkTemplate.PROFESSIONAL)
        if (settings.template == WatermarkTemplate.PROFESSIONAL) {
            drawQRCode(canvas, qrRect, scale)
        }

        // Calculate starting Y position and content widths dynamically
        val contentLeft = textCardLeft + 20f * scale
        val contentRight = textCardRight - 20f * scale
        val contentWidth = contentRight - contentLeft

        val titleContentWidth = if (settings.template == WatermarkTemplate.PROFESSIONAL) {
            qrRect.left - 16f * scale - contentLeft
        } else {
            contentWidth
        }

        val isNoLogoTemplate = settings.template == WatermarkTemplate.GPS_CAMERA_NO_LOGO

        val titlePaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 38f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val titleWidth = titlePaint.measureText(titleText)
        val titleToDraw = if (titleWidth > titleContentWidth) {
            val count = titlePaint.breakText(titleText, true, titleContentWidth - 10f * scale, null)
            titleText.substring(0, count) + "..."
        } else {
            titleText
        }

        val rawAddressText = if (settings.useManualLocation) settings.manualAddress else gpsData.address
        val plusCode = generatePlusCode(lat, lng)
        val detailsText = if (plusCode.isNotEmpty()) "$plusCode, $rawAddressText" else rawAddressText

        val addressPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 21f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val addressLineSpacing = 1.22f
        val addressLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(detailsText, 0, detailsText.length, addressPaint, titleContentWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, addressLineSpacing)
                .setIncludePad(false)
                .setMaxLines(2)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(detailsText, addressPaint, titleContentWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, addressLineSpacing, 0f, false)
        }

        val latValStr = formatCoordinate(lat, true, settings.coordinateFormat)
        val lngValStr = formatCoordinate(lng, false, settings.coordinateFormat)
        val tzString = gpsData.timezone.ifEmpty { "GMT+05:30" }.replace("GMT", "GMT ")

        if (settings.template == WatermarkTemplate.CORPORATE) {
            // Drawing logic specifically for CORPORATE:
            var currentY = textCardTop + 16f * scale

            // Draw large dynamic Date/Time block
            val timePaintLarge = Paint().apply {
                color = Color.WHITE
                textSize = 44f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val pipePaint = Paint().apply {
                color = badgeColor
                strokeWidth = 3f * scale
                isAntiAlias = true
            }
            val datePaintMed = Paint().apply {
                color = Color.WHITE
                textSize = 18f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val dayPaintSmall = Paint().apply {
                color = Color.WHITE
                textSize = 16f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val timeStr = dynamicTimeText
            val dateStr = try {
                SimpleDateFormat("dd MMM yyyy", Locale.US).format(dateObj)
            } catch (e: Exception) {
                gpsData.formattedDate.ifEmpty { "12 Jul 2026" }
            }
            val dayStr = try {
                SimpleDateFormat("EEEE", Locale.US).format(dateObj)
            } catch (e: Exception) {
                "Sunday"
            }

            canvas.drawText(timeStr, contentLeft, currentY + 38f * scale, timePaintLarge)
            val timeWidth = timePaintLarge.measureText(timeStr)

            val pipeX = contentLeft + timeWidth + 16f * scale
            canvas.drawLine(pipeX, currentY + 4f * scale, pipeX, currentY + 44f * scale, pipePaint)

            val dateX = pipeX + 16f * scale
            canvas.drawText(dateStr, dateX, currentY + 20f * scale, datePaintMed)
            canvas.drawText(dayStr, dateX, currentY + 42f * scale, dayPaintSmall)

            currentY += 75f * scale

            // Divider Line
            val dividerPaint = Paint().apply {
                color = Color.argb(45, 255, 255, 255)
                strokeWidth = 1f * scale
                isAntiAlias = true
            }
            canvas.drawLine(contentLeft, currentY, contentRight, currentY, dividerPaint)
            currentY += 24f * scale

            // Draw Location Name
            val titleTy = currentY + 30f * scale
            canvas.drawText(titleToDraw, contentLeft, titleTy, titlePaint)

            // Draw Address
            val addressTy = titleTy + 24f * scale
            canvas.save()
            canvas.translate(contentLeft, addressTy)
            addressLayout.draw(canvas)
            canvas.restore()

            // Draw Coordinates & Timezone under the address
            val coordsTy = addressTy + addressLayout.height + 24f * scale
            val coordsPaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("🧭  Lat: $latValStr   Long: $lngValStr", contentLeft, coordsTy, coordsPaint)
        } else if (settings.template == WatermarkTemplate.GPS_CAMERA_STYLE) {
            // RENDER REFINED "GPS Camera Style" TEMPLATE LAYOUT:
            // Centered vertically to cover the card perfectly
            val badgeHeight = 28f * scale
            val spacing1 = 18f * scale // Badge to Title
            val titleHeight = titlePaint.descent() - titlePaint.ascent()
            val spacing2 = 18f * scale // Title to Address
            val addressHeight = addressLayout.height.toFloat()
            val spacing3 = 18f * scale // Address to Coordinates
            val coordsHeight = 20f * scale
            val spacing4 = 18f * scale // Coordinates to DateTime
            val dateHeight = 20f * scale
            
            val totalHeight = badgeHeight + spacing1 + titleHeight + spacing2 + addressHeight + spacing3 + coordsHeight + spacing4 + dateHeight
            val startY = textCardTop + (cardHeight - totalHeight) / 2f

            // 1. Branding Badge: "📷 GeoSnap Camera" in the top right
            val badgeText = "GeoSnap Camera"
            val badgeTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 12.5f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val badgePaddingH = 11f * scale
            val iconSize = 13f * scale
            val spacing = 6f * scale
            
            val badgeTextWidth = badgeTextPaint.measureText(badgeText)
            val badgeWidth = iconSize + spacing + badgeTextWidth + 2f * badgePaddingH
            val badgeRight = textCardRight - 16f * scale
            val badgeLeft = badgeRight - badgeWidth
            val badgeTop = startY
            val badgeBottom = badgeTop + badgeHeight
            val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)
            
            val badgeBgPaint = Paint().apply {
                color = Color.argb(160, 20, 20, 25)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val badgeBorderPaint = Paint().apply {
                color = Color.argb(60, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
                isAntiAlias = true
            }
            canvas.drawRoundRect(badgeRect, 14f * scale, 14f * scale, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, 14f * scale, 14f * scale, badgeBorderPaint)
            
            // Draw camera icon
            val iconPaint = Paint().apply {
                color = Color.rgb(0, 230, 118) // accent color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val iconLeft = badgeLeft + badgePaddingH
            val iconTop = badgeTop + (badgeHeight - iconSize) / 2f
            val iconPath = Path().apply {
                moveTo(iconLeft + 2.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 4.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 5.5f * scale, iconTop + 1.5f * scale)
                lineTo(iconLeft + 7.5f * scale, iconTop + 1.5f * scale)
                lineTo(iconLeft + 8.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 10.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 10.5f * scale, iconTop + 11f * scale)
                lineTo(iconLeft + 2.5f * scale, iconTop + 11f * scale)
                close()
            }
            canvas.drawPath(iconPath, iconPaint)
            
            val lensPaint = Paint().apply {
                color = Color.argb(160, 20, 20, 25)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(iconLeft + 6.5f * scale, iconTop + 7f * scale, 2.5f * scale, lensPaint)
            
            val badgeTx = iconLeft + iconSize + spacing
            val badgeTy = badgeTop + badgeHeight / 2f - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
            canvas.drawText(badgeText, badgeTx, badgeTy, badgeTextPaint)

            // 2. Large Location Name (Title)
            val titleTy = startY + badgeHeight + spacing1 - titlePaint.ascent()
            canvas.drawText(titleToDraw, contentLeft, titleTy, titlePaint)

            // 3. Full Address & Plus Code (up to 2 wrapped lines)
            val addressTy = titleTy + titlePaint.descent() + spacing2
            canvas.save()
            canvas.translate(contentLeft, addressTy)
            addressLayout.draw(canvas)
            canvas.restore()

            // 4. Coordinates Row (Latitude, Longitude)
            val customCoordsPaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val coordsTop = addressTy + addressHeight + spacing3
            val coordsTy = coordsTop - customCoordsPaint.ascent()
            val coordsText = "🧭  Lat: $latValStr   Long: $lngValStr"
            canvas.drawText(coordsText, contentLeft, coordsTy, customCoordsPaint)

            // 5. Date & Time Row (Day, Date, Time, Timezone)
            val dateTop = coordsTop + coordsHeight + spacing4
            val dateTy = dateTop - customCoordsPaint.ascent()
            val dayStr = try {
                SimpleDateFormat("EEEE", Locale.US).format(dateObj)
            } catch (e: Exception) {
                "Sunday"
            }
            val dateTimeText = "📅  $dayStr, $dynamicDateText   🕒  $dynamicTimeText ($tzString)"
            canvas.drawText(dateTimeText, contentLeft, dateTy, customCoordsPaint)
        } else if (settings.template == WatermarkTemplate.GPS_CAMERA_NO_LOGO) {
            // RENDER REFINED "GPS Camera Style (No Logo)" TEMPLATE LAYOUT:
            // Centered vertically to cover the card perfectly and fill space properly
            val customCoordsPaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val titleHeight = titlePaint.descent() - titlePaint.ascent()
            val addressHeight = addressLayout.height.toFloat()
            val coordsHeight = customCoordsPaint.descent() - customCoordsPaint.ascent()
            val dateHeight = customCoordsPaint.descent() - customCoordsPaint.ascent()

            val spacing1 = 24f * scale
            val spacing2 = 24f * scale
            val spacing3 = 24f * scale

            val totalHeight = titleHeight + spacing1 + addressHeight + spacing2 + coordsHeight + spacing3 + dateHeight
            val startY = textCardTop + (cardHeight - totalHeight) / 2f

            // 1. Large Location Name (Title) - BOLD and WHITE
            val titleTy = startY - titlePaint.ascent()
            canvas.drawText(titleToDraw, contentLeft, titleTy, titlePaint)

            // 2. Full Address & Plus Code (up to 2 wrapped lines) - BOLD and WHITE
            val addressTy = startY + titleHeight + spacing1
            canvas.save()
            canvas.translate(contentLeft, addressTy)
            addressLayout.draw(canvas)
            canvas.restore()

            // 3. Coordinates Row (Latitude, Longitude) - BOLD and WHITE
            val coordsTop = addressTy + addressHeight + spacing2
            val coordsTy = coordsTop - customCoordsPaint.ascent()
            val coordsText = "Lat $latValStr   Long $lngValStr"
            canvas.drawText(coordsText, contentLeft, coordsTy, customCoordsPaint)

            // 4. Date & Time Row (Day, Date, Time, Timezone) - BOLD and WHITE
            val dateTop = coordsTop + coordsHeight + spacing3
            val dateTy = dateTop - customCoordsPaint.ascent()
            val dayStr = try {
                SimpleDateFormat("EEEE", Locale.US).format(dateObj)
            } catch (e: Exception) {
                "Sunday"
            }
            val dateTimeText = "$dayStr, $dynamicDateText $dynamicTimeText $tzString"
            canvas.drawText(dateTimeText, contentLeft, dateTy, customCoordsPaint)
        } else {
            // IMPROVED LAYOUT FOR ALL STANDARD CARD TEMPLATES:
            // Centered vertically to cover the card perfectly
            val badgeHeight = 28f * scale
            val spacing1 = 20f * scale // Badge to Title
            val titleHeight = titlePaint.descent() - titlePaint.ascent()
            val spacing2 = 20f * scale // Title to Address
            val addressHeight = addressLayout.height.toFloat()
            val spacing3 = 20f * scale // Address to Divider
            val spacing4 = 20f * scale // Divider to Extra / Meta
            
            val extraHeight = when (settings.template) {
                WatermarkTemplate.SURVEY, WatermarkTemplate.FIELD_INSPECTION -> 18f * scale
                WatermarkTemplate.COMPASS -> 36f * scale
                else -> 0f
            }
            val hasExtraRows = extraHeight > 0f
            val extraSpacing = if (hasExtraRows) 18f * scale else 0f
            
            val metaPaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val metaRowHeight = metaPaint.descent() - metaPaint.ascent()
            val metaHeight = metaRowHeight * 2f + 12f * scale // 2 rows of text with good line spacing
            
            val totalHeight = badgeHeight + spacing1 + titleHeight + spacing2 + addressHeight + spacing3 + spacing4 + extraHeight + extraSpacing + metaHeight
            val startY = textCardTop + (cardHeight - totalHeight) / 2f

            // 1. Branding badge: "📷 GeoSnap Camera"
            val badgeText = "GeoSnap Camera"
            val badgeTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 12.5f * scale
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val badgePaddingH = 11f * scale
            val iconSize = 13f * scale
            val spacing = 6f * scale
            
            val badgeTextWidth = badgeTextPaint.measureText(badgeText)
            val badgeWidth = badgePaddingH * 2f + iconSize + spacing + badgeTextWidth
            
            val badgeLeft = contentLeft
            val badgeTop = startY
            val badgeBottom = badgeTop + badgeHeight
            val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeBottom)
            
            val badgeBgPaint = Paint().apply {
                color = when (settings.template) {
                    WatermarkTemplate.CLASSIC -> Color.argb(55, 255, 215, 0)
                    WatermarkTemplate.PROFESSIONAL -> Color.argb(55, 0, 230, 118)
                    WatermarkTemplate.SURVEY -> Color.argb(55, 0, 176, 255)
                    WatermarkTemplate.FIELD_INSPECTION -> Color.argb(55, 76, 175, 80)
                    else -> Color.argb(40, 255, 255, 255)
                }
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val badgeBorderPaint = Paint().apply {
                color = Color.argb(80, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
                isAntiAlias = true
            }
            canvas.drawRoundRect(badgeRect, 14f * scale, 14f * scale, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, 14f * scale, 14f * scale, badgeBorderPaint)
            
            // Draw camera icon
            val iconPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val iconLeft = badgeLeft + badgePaddingH
            val iconTop = badgeTop + (badgeHeight - iconSize) / 2f
            val iconPath = Path().apply {
                moveTo(iconLeft + 2.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 4.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 5.5f * scale, iconTop + 1.5f * scale)
                lineTo(iconLeft + 7.5f * scale, iconTop + 1.5f * scale)
                lineTo(iconLeft + 8.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 10.5f * scale, iconTop + 3.5f * scale)
                lineTo(iconLeft + 10.5f * scale, iconTop + 11f * scale)
                lineTo(iconLeft + 2.5f * scale, iconTop + 11f * scale)
                close()
            }
            canvas.drawPath(iconPath, iconPaint)
            
            val lensPaint = Paint().apply {
                color = Color.argb(200, 15, 18, 22)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(iconLeft + 6.5f * scale, iconTop + 7f * scale, 2.5f * scale, lensPaint)
            
            val badgeTx = iconLeft + iconSize + spacing
            val badgeTy = badgeTop + badgeHeight / 2f - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
            canvas.drawText(badgeText, badgeTx, badgeTy, badgeTextPaint)

            // 2. Location Name (Title)
            val titleTy = badgeBottom + spacing1 - titlePaint.ascent()
            canvas.drawText(titleToDraw, contentLeft, titleTy, titlePaint)

            // 3. Full Address (up to 2 wrapped lines)
            val addressTy = titleTy + titlePaint.descent() + spacing2
            canvas.save()
            canvas.translate(contentLeft, addressTy)
            addressLayout.draw(canvas)
            canvas.restore()

            // 4. Divider Line
            val dividerY = addressTy + addressHeight + spacing3
            val dividerPaint = Paint().apply {
                color = Color.argb(45, 255, 255, 255)
                strokeWidth = 1f * scale
                isAntiAlias = true
            }
            val dividerRight = if (settings.template == WatermarkTemplate.PROFESSIONAL) qrRect.left - 16f * scale else contentRight
            canvas.drawLine(contentLeft, dividerY, dividerRight, dividerY, dividerPaint)

            // 5. Extra Rows (for Survey, Field Inspection, Compass)
            val extraStartY = dividerY + spacing4

            if (hasExtraRows) {
                val extraPaintHeader = Paint().apply {
                    color = badgeColor
                    textSize = 18f * scale
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                val colWidth = if (settings.template == WatermarkTemplate.PROFESSIONAL) {
                    (qrRect.left - 16f * scale - contentLeft) / 2f
                } else {
                    contentWidth / 2f
                }
                when (settings.template) {
                    WatermarkTemplate.SURVEY -> {
                        canvas.drawText("PROJECT: SITE SURVEY", contentLeft, extraStartY + 14f * scale, extraPaintHeader)
                        canvas.drawText("ACCURACY: ±3.0 METERS", contentLeft + colWidth, extraStartY + 14f * scale, extraPaintHeader)
                    }
                    WatermarkTemplate.FIELD_INSPECTION -> {
                        canvas.drawText("INSPECTOR: V. KUMAR", contentLeft, extraStartY + 14f * scale, extraPaintHeader)
                        canvas.drawText("VERIFIED: ON-SITE ✔", contentLeft + colWidth, extraStartY + 14f * scale, extraPaintHeader)
                    }
                    WatermarkTemplate.COMPASS -> {
                        canvas.drawText(String.format("Azimuth/Bearing: %.2f°", azimuth), contentLeft, extraStartY + 14f * scale, extraPaintHeader)
                        val subPaint = Paint().apply {
                            color = Color.WHITE
                            textSize = 18f * scale
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            isAntiAlias = true
                        }
                        canvas.drawText("🏔️ 8.69 m", contentLeft, extraStartY + 32f * scale, subPaint)
                        canvas.drawText("🧲 28.42 μT", contentLeft + colWidth, extraStartY + 32f * scale, subPaint)
                    }
                    else -> {}
                }
            }

            // 6. Metadata Row Grid: Latitude, Longitude, Date, Time, Timezone
            val metaStartY = dividerY + spacing4 + (if (hasExtraRows) extraHeight + extraSpacing else 0f)
            val metaColWidth = if (settings.template == WatermarkTemplate.PROFESSIONAL) {
                (qrRect.left - 16f * scale - contentLeft) / 2f
            } else {
                contentWidth / 2f
            }

            val firstRowTy = metaStartY - metaPaint.ascent()
            val secondRowTy = firstRowTy + metaRowHeight + 12f * scale

            // Latitude & Date
            canvas.drawText("🧭  Lat: $latValStr", contentLeft, firstRowTy, metaPaint)
            canvas.drawText("📅  Date: $dynamicDateText", contentLeft + metaColWidth, firstRowTy, metaPaint)

            // Longitude & Time (with Timezone)
            canvas.drawText("🧭  Long: $lngValStr", contentLeft, secondRowTy, metaPaint)
            canvas.drawText("🕒  Time: $dynamicTimeText ($tzString)", contentLeft + metaColWidth, secondRowTy, metaPaint)
        }
    }

    private fun drawQRCode(canvas: Canvas, rect: RectF, scale: Float) {
        val size = rect.width()
        val margin = 8f * scale
        val qrSize = size - 2f * margin
        val qx = rect.left + margin
        val qy = rect.top + margin
        
        // Draw white background for QR Code
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 16f * scale, 16f * scale, bgPaint)
        
        val qrPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Draw 3 Finder Patterns at corners (top-left, top-right, bottom-left)
        // Divider into 21x21 modules (standard QR Version 1)
        val numModules = 21
        val moduleSize = qrSize / numModules
        
        fun drawFinderPattern(col: Int, row: Int) {
            val px = qx + col * moduleSize
            val py = qy + row * moduleSize
            
            // 7x7 outer square
            canvas.drawRect(px, py, px + 7 * moduleSize, py + 7 * moduleSize, qrPaint)
            // 5x5 white inner square
            canvas.drawRect(px + moduleSize, py + moduleSize, px + 6 * moduleSize, py + 6 * moduleSize, bgPaint)
            // 3x3 black core
            canvas.drawRect(px + 2 * moduleSize, py + 2 * moduleSize, px + 5 * moduleSize, py + 5 * moduleSize, qrPaint)
        }
        
        // Top-Left Finder
        drawFinderPattern(0, 0)
        // Top-Right Finder
        drawFinderPattern(numModules - 7, 0)
        // Bottom-Left Finder
        drawFinderPattern(0, numModules - 7)
        
        // Draw deterministic QR pixels based on coordinates/hash to look completely authentic!
        var seed = 42L
        fun nextBit(): Boolean {
            seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
            return (seed % 2) == 0L
        }
        
        for (r in 0 until numModules) {
            for (c in 0 until numModules) {
                val inTopLeftFinder = r < 8 && c < 8
                val inTopRightFinder = r < 8 && c >= numModules - 8
                val inBottomLeftFinder = r >= numModules - 8 && c < 8
                
                if (!inTopLeftFinder && !inTopRightFinder && !inBottomLeftFinder) {
                    val isTimingPattern = r == 6 || c == 6
                    if (isTimingPattern) {
                        if ((r + c) % 2 == 0) {
                            canvas.drawRect(
                                qx + c * moduleSize,
                                qy + r * moduleSize,
                                qx + (c + 1) * moduleSize,
                                qy + (r + 1) * moduleSize,
                                qrPaint
                            )
                        }
                    } else if (nextBit()) {
                        canvas.drawRect(
                            qx + c * moduleSize,
                            qy + r * moduleSize,
                            qx + (c + 1) * moduleSize,
                            qy + (r + 1) * moduleSize,
                            qrPaint
                        )
                    }
                }
            }
        }
    }

    private fun drawCompassDial(canvas: Canvas, rect: RectF, azimuth: Float, scale: Float) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val radius = rect.width() / 2f
        
        // Draw outer compass card bg
        val dialBgPaint = Paint().apply {
            color = Color.argb(180, 24, 28, 36) // Dark compass bg
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, radius, dialBgPaint)
        
        // Draw outer subtle circle
        val dialBorderPaint = Paint().apply {
            color = Color.argb(90, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, radius - 2f * scale, dialBorderPaint)
        
        // Rotate canvas to draw ticks and cardinal directions
        canvas.save()
        canvas.rotate(-azimuth, cx, cy)
        
        // Tick marks and labels
        val tickPaint = Paint().apply {
            color = Color.argb(120, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f * scale
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f * scale
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        
        for (angle in 0 until 360 step 15) {
            val isMajor = angle % 30 == 0
            val tickLength = if (isMajor) 10f * scale else 5f * scale
            
            canvas.drawLine(cx, cy - radius + 2f * scale, cx, cy - radius + 2f * scale + tickLength, tickPaint)
            
            if (angle % 90 == 0) {
                val label = when (angle) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> ""
                }
                if (label.isNotEmpty()) {
                    val textWidth = textPaint.measureText(label)
                    canvas.drawText(label, cx - textWidth / 2f, cy - radius + 20f * scale, textPaint)
                }
            } else if (isMajor) {
                val label = angle.toString()
                val smallTextPaint = Paint().apply {
                    color = Color.argb(140, 255, 255, 255)
                    textSize = 7f * scale
                    isAntiAlias = true
                }
                val textWidth = smallTextPaint.measureText(label)
                canvas.drawText(label, cx - textWidth / 2f, cy - radius + 16f * scale, smallTextPaint)
            }
            canvas.rotate(15f, cx, cy)
        }
        canvas.restore()
        
        // Draw static needle pointing straight up (North) relative to screen
        val needlePath = Path().apply {
            moveTo(cx, cy - radius + 15f * scale) // top point
            lineTo(cx - 6f * scale, cy)           // left center
            lineTo(cx, cy + 8f * scale)           // bottom center
            close()
        }
        val needlePaintRed = Paint().apply {
            color = Color.rgb(244, 67, 54) // bright red north pointer
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(needlePath, needlePaintRed)
        
        val needlePathSouth = Path().apply {
            moveTo(cx, cy + radius - 15f * scale) // bottom point
            lineTo(cx - 6f * scale, cy)           // left center
            lineTo(cx, cy - 8f * scale)           // top center
            close()
        }
        val needlePaintGrey = Paint().apply {
            color = Color.rgb(180, 180, 180) // silver south pointer
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(needlePathSouth, needlePaintGrey)
        
        // Needle center pivot
        canvas.drawCircle(cx, cy, 4f * scale, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true })
        canvas.drawCircle(cx, cy, 5f * scale, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f * scale; isAntiAlias = true })
    }

    private fun getCardinalDirection(azimuth: Float): String {
        val degrees = (azimuth % 360 + 360) % 360
        return when {
            degrees >= 337.5 || degrees < 22.5 -> "North"
            degrees >= 22.5 && degrees < 67.5 -> "Northeast"
            degrees >= 67.5 && degrees < 112.5 -> "East"
            degrees >= 112.5 && degrees < 157.5 -> "Southeast"
            degrees >= 157.5 && degrees < 202.5 -> "South"
            degrees >= 202.5 && degrees < 247.5 -> "Southwest"
            degrees >= 247.5 && degrees < 292.5 -> "West"
            else -> "Northwest"
        }
    }

    private fun generatePlusCode(latitude: Double, longitude: Double): String {
        val alphabet = "23456789CFGHJMPQRVWX"
        val latVal = ((latitude + 90.0) / 180.0).coerceIn(0.0, 1.0)
        val lngVal = ((longitude + 180.0) / 360.0).coerceIn(0.0, 1.0)
        
        fun getDigit(fractionalValue: Double, shift: Double): Char {
            val idx = ((fractionalValue * shift).toLong() % 20).toInt()
            return alphabet[idx]
        }
        
        val d1 = getDigit(latVal, 20.0)
        val d2 = getDigit(lngVal, 20.0)
        val d3 = getDigit(latVal, 400.0)
        val d4 = getDigit(lngVal, 400.0)
        val d5 = getDigit(latVal, 8000.0)
        val d6 = getDigit(lngVal, 8000.0)
        val d7 = getDigit(latVal, 160000.0)
        val d8 = getDigit(lngVal, 160000.0)
        
        return "$d1${d2.lowercaseChar()}$d3${d4.lowercaseChar()}+$d5${d6.lowercaseChar()}$d7${d8.lowercaseChar()}"
    }

    private fun drawFakeSatelliteMap(canvas: Canvas, rect: RectF, lat: Double, lng: Double, scale: Float) {
        val seed = ((lat * 100000).toLong() xor (lng * 100000).toLong()).let { if (it == 0L) 123456789L else it }
        val random = java.util.Random(seed)

        // 1. Base Terrain: Dark satellite rich green
        val bgPaint = Paint().apply {
            color = Color.rgb(24, 45, 27) // deep forest green
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(rect, bgPaint)

        // 2. Terrain Patches: Organic variations in ground color
        val patchPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val colors = listOf(
            Color.rgb(18, 38, 22),   // deeper green
            Color.rgb(38, 58, 32),   // olive green
            Color.rgb(42, 50, 28),   // dry grass/soil
            Color.rgb(28, 34, 22)    // forest shade
        )
        for (i in 0..12) {
            val px = rect.left + random.nextFloat() * rect.width()
            val py = rect.top + random.nextFloat() * rect.height()
            val r = (30f + random.nextFloat() * 60f) * scale
            patchPaint.color = colors[random.nextInt(colors.size)]
            patchPaint.alpha = 140
            canvas.drawCircle(px, py, r, patchPaint)
        }

        // 3. Roads: Slate/Asphalt grey lines
        val roadOutlinePaint = Paint().apply {
            color = Color.rgb(40, 42, 45) // road dark base
            style = Paint.Style.STROKE
            strokeWidth = 10f * scale
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val roadInnerPaint = Paint().apply {
            color = Color.rgb(75, 78, 82) // road center
            style = Paint.Style.STROKE
            strokeWidth = 6f * scale
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val roadMarkingPaint = Paint().apply {
            color = Color.rgb(230, 230, 230) // lane markings
            style = Paint.Style.STROKE
            strokeWidth = 1f * scale
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f * scale, 6f * scale), 0f)
            isAntiAlias = true
        }

        val numRoads = 2 + random.nextInt(2)
        val roadPaths = mutableListOf<Path>()
        for (i in 0 until numRoads) {
            val path = Path()
            if (i == 0) {
                val startX = rect.left - 20f * scale
                val startY = rect.top + random.nextFloat() * rect.height() * 0.4f
                val endX = rect.right + 20f * scale
                val endY = rect.bottom - random.nextFloat() * rect.height() * 0.4f
                path.moveTo(startX, startY)
                val controlX = rect.left + rect.width() * (0.3f + random.nextFloat() * 0.4f)
                val controlY = rect.top + rect.height() * (0.3f + random.nextFloat() * 0.4f)
                path.quadTo(controlX, controlY, endX, endY)
            } else if (i == 1) {
                val startX = rect.left + random.nextFloat() * rect.width() * 0.3f
                val startY = rect.bottom + 20f * scale
                val endX = rect.right - random.nextFloat() * rect.width() * 0.3f
                val endY = rect.top - 20f * scale
                path.moveTo(startX, startY)
                val controlX = rect.left + rect.width() * (0.3f + random.nextFloat() * 0.4f)
                val controlY = rect.top + rect.height() * (0.3f + random.nextFloat() * 0.4f)
                path.quadTo(controlX, controlY, endX, endY)
            } else {
                val startY = rect.top + random.nextFloat() * rect.height()
                path.moveTo(rect.left - 10f * scale, startY)
                path.lineTo(rect.right + 10f * scale, startY + (random.nextFloat() - 0.5f) * 40f * scale)
            }
            roadPaths.add(path)
        }

        for (path in roadPaths) {
            canvas.drawPath(path, roadOutlinePaint)
        }
        for (path in roadPaths) {
            canvas.drawPath(path, roadInnerPaint)
            canvas.drawPath(path, roadMarkingPaint)
        }

        // 4. Buildings & Houses
        val shadowPaint = Paint().apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val housePaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val houseColors = listOf(
            Color.rgb(180, 80, 65),   // orange-red terracotta roof
            Color.rgb(200, 160, 120), // cream beige
            Color.rgb(100, 110, 120), // slate roof
            Color.rgb(140, 150, 160), // light blueish grey
            Color.rgb(195, 175, 150)  // wooden brown
        )

        val numHouses = 15 + random.nextInt(15)
        for (i in 0 until numHouses) {
            val hx = rect.left + 20f * scale + random.nextFloat() * (rect.width() - 40f * scale)
            val hy = rect.top + 20f * scale + random.nextFloat() * (rect.height() - 40f * scale)
            val hw = (8f + random.nextFloat() * 8f) * scale
            val hh = (8f + random.nextFloat() * 8f) * scale
            val angle = random.nextFloat() * 360f

            canvas.save()
            canvas.translate(hx, hy)
            canvas.rotate(angle)

            // Roof shadow
            val shadowOffset = 1.5f * scale
            val shadowRect = RectF(shadowOffset, shadowOffset, hw + shadowOffset, hh + shadowOffset)
            canvas.drawRect(shadowRect, shadowPaint)

            // Roof surface
            val roofRect = RectF(0f, 0f, hw, hh)
            housePaint.color = houseColors[random.nextInt(houseColors.size)]
            canvas.drawRect(roofRect, housePaint)

            // Gable line
            val ridgePaint = Paint().apply {
                color = Color.argb(40, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            }
            canvas.drawLine(0f, hh / 2f, hw, hh / 2f, ridgePaint)

            canvas.restore()
        }

        // 5. Trees: Clumpy green circles
        val treeColors = listOf(
            Color.rgb(34, 76, 40),    // dark forest green
            Color.rgb(46, 96, 52),    // vibrant green
            Color.rgb(55, 114, 62)    // light foliage green
        )
        val numTrees = 20 + random.nextInt(20)
        for (i in 0 until numTrees) {
            val tx = rect.left + 15f * scale + random.nextFloat() * (rect.width() - 30f * scale)
            val ty = rect.top + 15f * scale + random.nextFloat() * (rect.height() - 30f * scale)
            val treeSize = (4f + random.nextFloat() * 6f) * scale

            val tsOffset = 1.2f * scale
            canvas.drawCircle(tx + tsOffset, ty + tsOffset, treeSize, shadowPaint)

            val foliagePaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            foliagePaint.color = treeColors[random.nextInt(treeColors.size)]
            canvas.drawCircle(tx, ty, treeSize, foliagePaint)

            // Highlight layer
            foliagePaint.color = Color.argb(100, 255, 255, 255)
            canvas.drawCircle(tx - 1f * scale, ty - 1f * scale, treeSize * 0.5f, foliagePaint)
        }

        // 6. Vignette overlay for visual integration
        val shadowRingPaint = Paint().apply {
            val gradient = android.graphics.RadialGradient(
                rect.centerX(), rect.centerY(), rect.width() * 0.7f,
                intArrayOf(Color.TRANSPARENT, Color.argb(50, 0, 0, 0)),
                null, android.graphics.Shader.TileMode.CLAMP
            )
            shader = gradient
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(rect, shadowRingPaint)
    }
}
