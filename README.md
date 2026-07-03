# Noveliq

Noveliq is an Android client for [Audiobookshelf](https://www.audiobookshelf.org/), an open-source self-hosted audiobook and podcast server.

## Features

Implemented:

- [x] Connect to your Audiobookshelf server (URL validation and health check).
- [x] Log in and persist your session across app launches.
- [x] Browse libraries and audiobooks, cached locally for offline-first reads.
- [x] Continue Listening shelf on the home dashboard.
- [x] Audiobook detail screen with chapters and cached track metadata.
- [x] Stream and play audiobooks with background playback, a media notification, and media session controls.

Planned:

- [ ] Search and filter your library.
- [ ] Download content for offline listening.
- [ ] Track playback progress and sync it with the server.
- [ ] Support for multiple libraries (single-library selection today).
- [ ] Sleep timer and playback speed control.
- [ ] Android Auto and Wear OS support.

## Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3
- **Architecture:** layered MVVM (Model-View-ViewModel)
- **Dependency Injection:** Hilt
- **Networking:** Retrofit + OkHttp
- **Local Database:** Room
- **Media Playback:** Media3 / ExoPlayer (`MediaLibraryService` + `MediaSession`)

## Getting Started

### Prerequisites

- Android Studio Jellyfish or newer.
- Android SDK 24+.
- An active Audiobookshelf server instance.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/evoionosp/noveliq-android.git
   ```
2. Open the project in Android Studio.
3. Build and run the app on your device or emulator.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
