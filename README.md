# Anilover

[![Release](https://img.shields.io/github/v/release/adiytharpansa/anilover)](https://github.com/adiytharpansa/anilover/releases)
[![Build Status](https://github.com/adiytharpansa/anilover/actions/workflows/build.yml/badge.svg)](https://github.com/adiytharpansa/anilover/actions)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Anilover** is an Android application for streaming anime content with HLS streaming support, built using modern Android development tools.

---

## Download

Download the latest APK from the **[Releases](https://github.com/adiytharpansa/anilover/releases)** page.

---

## Features

### Streaming
- HLS streaming with adaptive bitrate support
- CDN-based content delivery via jsDelivr
- Automatic retry mechanism for failed segments
- Optimized buffering for smooth playback

### User Interface
- Modern home screen with hero banner and category browsing
- Continue watching with progress tracking
- Episode list with individual progress indicators
- Detailed metadata display for each title

### Video Player
- ExoPlayer (Media3) based player
- Double-tap to seek forward/backward
- Playback speed control
- Audio and subtitle selection
- Picture-in-Picture mode
- Screen lock functionality
- Gesture controls for brightness, volume, and seek

### Offline Capabilities
- Download episodes for offline viewing
- Progress tracking for downloaded content
- Automatic cleanup of failed downloads

---

## Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Minimum SDK | 24 (Android 7.0+) |
| Media Playback | ExoPlayer (Media3) |
| Network | Retrofit, OkHttp |
| Dependency Injection | Hilt |
| Architecture | MVVM |

---

## Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/adiytharpansa/anilover.git
   ```

2. Open in Android Studio

3. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```

Note: The app uses a debug keystore for signing. For production releases, configure your own signing configuration.

---

## Configuration

The app requires a `CATALOG_KEY` environment variable for full functionality. Set this in your GitHub Secrets or local environment.

---

## Changelog

Detailed change history is available in **[CATATAN-PERUBAHAN-UI.md](CATATAN-PERUBAHAN-UI.md)**

---

## Contributing

Contributions are welcome. Please fork the repository and submit a pull request with your changes.

---

## License

This project is licensed under the MIT License - see the **[LICENSE](LICENSE)** file for details.
