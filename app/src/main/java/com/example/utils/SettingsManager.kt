package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SettingsData
import com.example.model.WatermarkTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("geosnap_camera_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()

    private fun loadSettings(): SettingsData {
        val templateStr = sharedPrefs.getString("template", WatermarkTemplate.GPS_CAMERA_NO_LOGO.name) ?: WatermarkTemplate.GPS_CAMERA_NO_LOGO.name
        val template = try {
            WatermarkTemplate.valueOf(templateStr)
        } catch (e: Exception) {
            WatermarkTemplate.GPS_CAMERA_NO_LOGO
        }

        return SettingsData(
            template = template,
            useManualLocation = sharedPrefs.getBoolean("use_manual_location", false),
            manualLatitude = sharedPrefs.getFloat("manual_latitude", 24.66655f).toDouble(),
            manualLongitude = sharedPrefs.getFloat("manual_longitude", 87.69474f).toDouble(),
            manualAddress = sharedPrefs.getString("manual_address", "Baghsisa, Jharkhand 816104, India") ?: "Baghsisa, Jharkhand 816104, India",
            manualCity = sharedPrefs.getString("manual_city", "Baghsisa") ?: "Baghsisa",
            manualCountry = sharedPrefs.getString("manual_country", "India") ?: "India",
            dateFormat = sharedPrefs.getString("date_format", "dd/MM/yyyy") ?: "dd/MM/yyyy",
            timeFormat = sharedPrefs.getString("time_format", "hh:mm a") ?: "hh:mm a",
            coordinateFormat = sharedPrefs.getString("coordinate_format", "DD") ?: "DD",
            showCountryFlag = sharedPrefs.getBoolean("show_country_flag", true),
            showMiniMap = sharedPrefs.getBoolean("show_mini_map", true),
            showCompass = sharedPrefs.getBoolean("show_compass", true),
            watermarkPosition = sharedPrefs.getString("watermark_position", "Bottom") ?: "Bottom",
            watermarkSize = sharedPrefs.getString("watermark_size", "Medium") ?: "Medium",
            opacity = sharedPrefs.getFloat("opacity", 0.7f),
            roundedCorners = sharedPrefs.getBoolean("rounded_corners", true),
            glassBlur = sharedPrefs.getBoolean("glass_blur", true),
            shutterSound = sharedPrefs.getBoolean("shutter_sound_enabled", true),
            saveLocation = sharedPrefs.getString("save_location", "GeoSnap") ?: "GeoSnap",
            theme = sharedPrefs.getString("theme", "Dark") ?: "Dark",
            language = sharedPrefs.getString("language", "en") ?: "en",
            imageQuality = sharedPrefs.getString("image_quality", "High") ?: "High"
        )
    }

    fun updateSetting(updater: SettingsData.() -> SettingsData) {
        val current = _settings.value
        val updated = current.updater()
        _settings.value = updated
        saveSettings(updated)
    }

    private fun saveSettings(data: SettingsData) {
        sharedPrefs.edit().apply {
            putString("template", data.template.name)
            putBoolean("use_manual_location", data.useManualLocation)
            putFloat("manual_latitude", data.manualLatitude.toFloat())
            putFloat("manual_longitude", data.manualLongitude.toFloat())
            putString("manual_address", data.manualAddress)
            putString("manual_city", data.manualCity)
            putString("manual_country", data.manualCountry)
            putString("date_format", data.dateFormat)
            putString("time_format", data.timeFormat)
            putString("coordinate_format", data.coordinateFormat)
            putBoolean("show_country_flag", data.showCountryFlag)
            putBoolean("show_mini_map", data.showMiniMap)
            putBoolean("show_compass", data.showCompass)
            putString("watermark_position", data.watermarkPosition)
            putString("watermark_size", data.watermarkSize)
            putFloat("opacity", data.opacity)
            putBoolean("rounded_corners", data.roundedCorners)
            putBoolean("glass_blur", data.glassBlur)
            putBoolean("shutter_sound_enabled", data.shutterSound)
            putString("save_location", data.saveLocation)
            putString("theme", data.theme)
            putString("language", data.language)
            putString("image_quality", data.imageQuality)
            apply()
        }
    }

    fun resetSettings() {
        sharedPrefs.edit().clear().apply()
        _settings.value = SettingsData()
    }
}
