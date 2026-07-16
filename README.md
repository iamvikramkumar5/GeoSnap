# 📸 GeoSnap — Professional GPS Watermark Camera

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-Modern_Camera_Engine-green.svg?style=flat)](https://developer.android.com/training/camerax)
[![Media3](https://img.shields.io/badge/Media3_Transformer-High--Fidelity_Video-orange.svg?style=flat)](https://developer.android.com/media/media3)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_%28API_26+%29-blue.svg?style=flat&logo=android)](https://developer.android.com)

**GeoSnap** is a high-performance, modern Android GPS Camera application built from the ground up using **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, and **Jetpack Media3**. It enables engineers, surveyors, real estate professionals, field inspectors, and outdoor enthusiasts to capture high-resolution photos and videos embedded with real-time, high-fidelity geolocated overlays, telemetry data, and customizable map tiles.

---

## ✨ Features

### 📍 Precise Telemetry & Location Overlays
- **Real-Time Data**: Seamlessly embeds accurate **Latitude, Longitude, Altitude, Speed**, and **Compass Azimuth** directly onto captured photos and videos.
- **Reverse Geocoding**: Automatically resolves coordinates to physical addresses (Street, City, State, Country) with matching interactive country flag emojis.
- **Accurate Directional Tracking**: Integrates device compass sensors to overlay precise azimuth readings.

### 🗺️ Dynamic Embedded Map Overlay
- **Multi-Style Maps**: Choose between **Satellite, Road**, and **Hybrid** map types to suit different environments.
- **Offline-First Cache**: Integrates a robust local map tile pre-fetch and caching engine. It caches tile views instantly, preventing synchronous, blocking network calls when snapping media.
- **Dynamic Preview**: Render mini-maps in adjustable shapes with custom rounded corners or professional alignments.

### 🎨 Customizable Watermark Templates
Tailored templates designed specifically for professional and casual workflows:
- **GPS Camera Style**: Classic layout with a bold map, location, and metadata bar.
- **Professional**: Clean, high-contrast layouts perfect for official reports.
- **Survey & Construction**: Specialized grids featuring precise coordinates, elevation data, and timezone metadata.
- **Field Inspection & Real Estate**: Maximized address visibility and layout clarity.
- **Compass & Simple**: Minimalist telemetry designs that keep the photo context front and center.
- **Dark, Light, Transparent**: Instantly adjust templates to complement any backdrop.

### 🎥 Media3 High-Fidelity Video Transcoding
- **On-the-Fly Overlays**: Leverages Google's next-generation **Media3 (Transformer)** library to stitch static map previews, custom telemetry texts, and templates directly onto recorded videos.
- **Main-Thread Safe**: Runs asynchronous background transcodes utilizing dedicated single-thread executors and handler loops, preventing UI stuttering and frame drops.
- **Graceful Fallback**: Automatically saves high-quality original recordings in the event of hardware transcode constraints.

### 🛠️ Advanced Settings & Simulators
- **Manual Location Override**: Simulate and test specific coordinates (Latitude/Longitude) and descriptive addresses—perfect for remote planning or demonstration.
- **Fine-Grain Configurations**: Customize date/time formatting, coordinate notation (Decimal Degrees vs. DMS), watermark opacity, glass blur background panels, and shutter sound controls.
- **Aesthetic Glassmorphism**: Floating control cards built with Material 3 dynamic glass-blur modifiers, light/dark responsive schemes, and edge-to-edge styling.

---

## 🏗️ Technical Architecture

GeoSnap adheres strictly to **Clean Architecture** and **MVVM (Model-View-ViewModel)** guidelines to ensure robust state management and high performance:

```
├── com.example
│   ├── MainActivity.kt               # Application Entry Point & Permissions Flow
│   ├── model
│   │   ├── GPSData.kt                # Geolocation and telemetry data model
│   │   └── SettingsData.kt           # Watermark preferences and app settings
│   ├── service
│   │   └── LocationService.kt        # Background FusedLocation updates and reverse-geocoding
│   ├── ui
│   │   ├── CameraViewModel.kt        # Primary camera, recording, and photo capture coordinator
│   │   ├── components
│   │   │   ├── CameraControls.kt     # Bottom bars, template selection, and shutter mechanics
│   │   │   ├── CameraPreviewView.kt  # CameraX PreviewView container with Compose lifecycle binding
│   │   │   ├── GPSOverlayCard.kt     # Fully composable Glassmorphic template selector & preview overlay
│   │   │   ├── GalleryView.kt        # Integrated media manager with photo/video playback
│   │   │   └── SettingsView.kt       # Complete settings panel with customization toggles
│   │   └── theme
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── utils
│       ├── MediaSaver.kt             # MediaStore gallery saver and Media3 Transformer coordinator
│       ├── SettingsManager.kt        # Persistent local SharedPreferences configuration
│       └── WatermarkGenerator.kt     # Core rendering engine utilizing Android Canvas API
```

### 🛠️ Tech Stack & Libraries
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3.
* **Camera Engine:** [CameraX](https://developer.android.com/training/camerax) (Core, Lifecycle, Video, View).
* **Video Processing:** [Media3 Transformer](https://developer.android.com/media/media3) & `OverlayEffect` for real-time video watermarking.
* **Location & Sensors:** Google Play Services Fused Location Provider and Android Hardware Sensor Manager (Accelerometer & Magnetometer).
* **Concurrency:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & `StateFlow` for fluid, non-blocking UI state rendering.

---

## 🗺️ How it Works (No API Keys Required)

One of GeoSnap's strongest assets is its **zero-friction setup**. You can build, open, and fully operate GeoSnap without setting up any expensive API keys:
1. **GPS Coordinates**: Resolved directly via Android’s built-in, low-power **FusedLocationProviderClient**.
2. **Reverse Geocoding**: Done locally on-device through Android's system `Geocoder`, avoiding costly external API quotas.
3. **Static Map Tiles**: Powered by an intelligent local image caching strategy and public static map tiles. The app automatically fetches tiles asynchronously and caches them instantly in memory, guaranteeing rapid, zero-lag watermarking without synchronous network blocking.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Koala / Ladybug** or newer.
- **Android SDK Platform 34** (targetSdk) or newer.
- **JDK 17**.

### Building and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/GeoSnap.git
   cd GeoSnap
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync dependencies.
4. Run the `:app` module on an Android Device or Emulator.

---

## 🔒 Permissions
To provide real-time overlays, the app declares and requests:
- `android.permission.CAMERA` — For video and photo capture.
- `android.permission.ACCESS_FINE_LOCATION` & `android.permission.ACCESS_COARSE_LOCATION` — For fetching real-time telemetry coordinates.
- `android.permission.RECORD_AUDIO` — For recording sound on watermarked videos.

---

## 🎨 Creative Showcase & Design Choices
- **Edge-to-Edge Canvas**: Employs full edge-to-edge layouts using `enableEdgeToEdge()` to maximize screen real estate, letting the live camera preview bleed beautifully into system status bars.
- **Premium Glassmorphism**: Semi-transparent, blur-backed Material 3 cards let the camera preview remain softly visible underneath telemetry layouts.
- **Fluid Typography**: Dynamic display fonts paired with monospace elements ensure that coordinate indicators are ultra-readable under bright sunlight or dark nighttime environments.

---

*Crafted with 💖 by Google AI Studio's Coding Agent.*
