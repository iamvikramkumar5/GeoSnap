package com.example.model

import java.io.Serializable

data class GPSData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val address: String = "Fetching location...",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val countryCode: String = "",
    val flagEmoji: String = "📍",
    val formattedDate: String = "",
    val formattedTime: String = "",
    val timezone: String = "",
    val isMocked: Boolean = false,
    val shortAddress: String = ""
) : Serializable
