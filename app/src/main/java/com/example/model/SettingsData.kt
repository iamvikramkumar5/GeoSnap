package com.example.model

import java.io.Serializable

enum class WatermarkTemplate {
    DEFAULT,
    CLASSIC,
    PROFESSIONAL,
    SURVEY,
    CONSTRUCTION,
    FIELD_INSPECTION,
    REAL_ESTATE,
    COMPASS,
    SIMPLE,
    DARK,
    TRANSPARENT,
    EMERGENCY,
    CORPORATE,
    GPS_CAMERA_STYLE,
    GPS_CAMERA_NO_LOGO
}

data class SettingsData(
    val template: WatermarkTemplate = WatermarkTemplate.GPS_CAMERA_NO_LOGO,
    val mapViewType: String = "Satellite", // "Satellite", "Road", "Hybrid"
    val useManualLocation: Boolean = false,
    val manualLatitude: Double = 24.66655,
    val manualLongitude: Double = 87.69474,
    val manualAddress: String = "Baghsisa, Jharkhand 816104, India",
    val manualCity: String = "Baghsisa",
    val manualCountry: String = "India",
    val dateFormat: String = "dd/MM/yyyy",
    val timeFormat: String = "hh:mm a",
    val coordinateFormat: String = "DD", // "DD" for Decimal Degrees, "DMS" for Degrees Minutes Seconds
    val showCountryFlag: Boolean = true,
    val showMiniMap: Boolean = true,
    val showCompass: Boolean = true,
    val watermarkPosition: String = "Bottom", // "Bottom" or "Top"
    val watermarkSize: String = "Medium", // "Small", "Medium", "Large"
    val opacity: Float = 0.7f,
    val roundedCorners: Boolean = true,
    val glassBlur: Boolean = true,
    val shutterSound: Boolean = true,
    val saveLocation: String = "GeoSnap",
    val theme: String = "Dark", // "Light", "Dark", "System"
    val language: String = "en",
    val imageQuality: String = "High" // "Low", "Medium", "High"
) : Serializable
