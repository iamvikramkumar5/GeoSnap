# 📸 GeoSnap — Professional GPS Watermark Camera

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-Modern_Camera_Engine-green.svg?style=flat)](https://developer.android.com/training/camerax)
[![Media3](https://img.shields.io/badge/Media3_Transformer-High--Fidelity_Video-orange.svg?style=flat)](https://developer.android.com/media/media3)
[![Platform](https://img.shields.io/badge/Platform-Android_7.0+_%28API_24+%29-blue.svg?style=flat&logo=android)](https://developer.android.com)

**GeoSnap** is a high-performance, modern Android GPS Camera application built from the ground up using **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX**, and **Jetpack Media3**. It enables engineers, surveyors, real estate professionals, field inspectors, and outdoor enthusiasts to capture high-resolution photos and videos embedded with real-time, high-fidelity geolocated overlays, telemetry data, and customizable map tiles.

---

## 👑 Lead Designer & Creator
This application was conceptualized, designed, and crafted with precision by:
### **VIKRAM KUMAR** 🚀
*Lead Designer & System Architect*

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

## 🚀 How to Open & Run in Android Studio

Follow these steps to set up, build, and run the project locally on your machine using Android Studio:

### 1. Prerequisites
Before importing the project, make sure you have:
* **Android Studio Ladybug (2024.2.1) / Koala (2024.1.1)** or newer.
* **JDK 17** configured as your Gradle JDK.
* **Android SDK Platform 34** (or higher) installed via the Android Studio SDK Manager.

---

### 2. Download or Clone the Repository
If you downloaded the code as a ZIP archive:
1. Extract the ZIP file into a dedicated project directory (e.g., `~/AndroidStudioProjects/GeoSnap`).

If you cloned the repository via Git:
```bash
git clone https://github.com/yourusername/GeoSnap.git
cd GeoSnap
```

---

### 3. Importing the Project into Android Studio
1. Launch **Android Studio**.
2. On the welcome screen, click **Open** (or go to **File -> Open** in an existing workspace).
3. Navigate to the directory where you extracted/cloned **GeoSnap** and select the root directory (the folder containing `build.gradle.kts` and `settings.gradle.kts`).
4. Click **OK**. Android Studio will begin initializing the project.

---

### 4. Let Gradle Sync Complete
Upon opening, Android Studio will automatically run a Gradle sync to resolve all the dependencies listed in `gradle/libs.versions.toml`.
* This may take a few minutes if you are importing it for the first time.
* Ensure you are connected to the Internet so Gradle can download the necessary libraries (Jetpack Compose, CameraX, Play Services, Media3, etc.).
* Once the sync completes successfully, you will see a green checkmark or `BUILD SUCCESSFUL` message in the Gradle sync tab.

---

### 5. Configuring the JDK (If build errors occur)
If you get JDK compatibility errors during the import, ensure Android Studio is using JDK 17:
1. Go to **File -> Settings** (Windows/Linux) or **Android Studio -> Settings** (macOS).
2. Navigate to **Build, Execution, Deployment -> Build Tools -> Gradle**.
3. Under **Gradle JDK**, select **Jetpack Runtime** or **JDK 17**.
4. Click **Apply** and then **OK**, then click **Sync Project with Gradle Files** in the top toolbar.

---

### 6. Run the App
Now you are ready to compile and run the app on an Android device or emulator:
1. Connect a physical Android device via USB (with **USB Debugging** enabled in Developer Options) OR launch a Virtual Device (AVD) from the Android Studio Device Manager.
2. In the top toolbar, ensure that the run configuration is set to **`:app`** and your target device is selected.
3. Click the green **Run (Play)** button, or press `Shift + F10` (Windows/Linux) / `Control + R` (macOS).
4. The Gradle build will run, generate the APK, and install the app onto your device.

---

---

## 🛠️ Code Customization & Development Guide (Code Editor / Android Studio)

Want to customize or modify **GeoSnap**? Here is a practical, developer-friendly guide on how to navigate the codebase, find key files, and modify them easily in the **AI Studio Web Code Editor** or locally in **Android Studio**.

### 📁 Key Files to Explore & Modify
Here is where the magic happens:

1. **Design Theme & Color Accents (App Theme)**
   * **Path:** `app/src/main/java/com/example/ui/theme/Color.kt` & `Theme.kt`
   * **Modification:** App ke primary/secondary colors badalne ke liye `Color.kt` mein custom Hex ranges update karein. `Theme.kt` check karein to configure Material 3 dynamic color switching.
2. **Watermark Card Overlay (On-screen Preview UI)**
   * **Path:** `app/src/main/java/com/example/ui/components/GPSOverlayCard.kt`
   * **Modification:** Screen par watermark overlay card ka layout, gradient styling, border rounding, or Glassmorphic blur modify karne ke liye iss Jetpack Compose view ko update karein.
3. **Image & Video Rendering Engine (Saving Watermarks to Files)**
   * **Path:** `app/src/main/java/com/example/utils/WatermarkGenerator.kt`
   * **Modification:** Saved JPEGs and recorded Videos par final watermark ko high-resolution draw karne ke liye core Android Canvas rendering algorithm ko customize karein (e.g., change fonts, add custom logo overlays, or shift coordinates mapping).
4. **Camera Controls & Bottom Actions UI**
   * **Path:** `app/src/main/java/com/example/ui/components/CameraControls.kt`
   * **Modification:** Custom capture button animations, templates selector slider padding, or flash toggle behavior control karne ke liye edit karein.
5. **App Preferences & User Settings Toggles**
   * **Path:** `app/src/main/java/com/example/ui/components/SettingsView.kt` & `SettingsManager.kt`
   * **Modification:** Custom toggle features create karein, dynamic speed thresholds coordinate styles add karein, and change how settings are stored on persistent SharedPreferences storage.

---

### 🔄 How to Modify the App Step-by-Step

#### 💻 Option A: Using AI Studio Web Code Editor (Instant Development)
1. **Find File**: Left-side code explorer tree view scroll karein and go to the target file.
2. **Code Edit**: Directly online text editor window mein lines modify karein (Jaise ki background color value or custom text styles).
3. **Auto Sync**: Editor compile aur build system trigger karega, and immediate updates automatic side Emulator preview window screen par sync hone lagenge!

#### 🚀 Option B: Locally in Android Studio (Full Native Control)
1. **Import Workspace**: Click **File -> Open**, navigate to project root directory, and pick `settings.gradle.kts` file.
2. **Fast File Find**: Keep typing `Shift` twice on keyboard (Search Everywhere tool) to find key classes like `WatermarkGenerator` or `GPSOverlayCard` instantly.
3. **Add Features**: Compose dynamic layouts, add system listeners, or perform Gradle updates using rich Android Studio tools.
4. **Build APK**: Generate a distributable installer via **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** to run on your own or client device!

---

## 🔒 Permissions
Upon opening, the application will prompt you for the following essential permissions. Please grant them to enjoy full functionality:
* **Camera Access** (`android.permission.CAMERA`) — To capture beautiful real-time watermarked photographs.
* **Location Access** (`android.permission.ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`) — To accurately capture current coordinates, elevation, speed, and addresses.
* **Audio Access** (`android.permission.RECORD_AUDIO`) — To record high-fidelity sound during video recordings.

---

## 🔮 Future Roadmap

We are constantly working to improve GeoSnap. Here is what's coming next:

* **🍏 iOS Support is Coming Soon!** 
  * We are planning to rewrite the core watermark rendering and settings module using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform** to deliver a fully native and synchronized experience across both **Android and iOS** devices.
* **☁️ Cloud Backup & Synchronization**: Sync captured photos and watermark templates across devices.
* **🏷️ EXIF Metadata Tags**: Store coordinates directly inside photo EXIF headers for desktop GIS systems.
* **📈 Enhanced Offline Map Overlays**: Support for vector-based offline map packs.

---

*Crafted with 💖 by **VIKRAM KUMAR** and Google AI Studio.*
