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

## Quality

### Local commands

| Command | What it does |
|---|---|
| `./gradlew ktlintCheck` | Verifies formatting against `.editorconfig` |
| `./gradlew ktlintFormat` | Fixes what it can automatically |
| `./gradlew :app:lintDebug` | Android Lint across all modules (`checkDependencies = true`) |
| `./gradlew test` | Unit tests only |
| `./gradlew koverHtmlReport` | Unit tests + coverage; report at `build/reports/kover/html/index.html` |
| `./gradlew koverVerify` | Enforces the coverage floors in `settings.gradle.kts` |

### Coverage

The target is **90-100% line coverage of unit-testable code**. Generated code
(Hilt/Room/KSP), Android entry points, and `@Composable` UI are excluded from the
measurement — see the filters in `settings.gradle.kts` — so the percentage reflects
logic that was left untested rather than framework glue that never could be.

Floors are a ratchet: `coverageFloors` in `settings.gradle.kts` holds a per-module
minimum. Raise a floor whenever a module clears it with room to spare; never lower
one. CI prints the current per-module numbers and the largest coverage gaps in each
run's job summary.

### Kotlin compilation

This project uses **AGP's built-in Kotlin support** (AGP 9.0+). There is deliberately
no `org.jetbrains.kotlin.android` plugin — AGP compiles Kotlin itself and registers the
`kotlin { }` extension, so `jvmToolchain` and `compilerOptions` are configured exactly
as before. Applying `kotlin-android` alongside it is a hard error, as is `kotlin-kapt`
(use KSP, which this project already does). The Compose compiler plugin is still applied
separately.

### Toolchains

Everything runs on **JDK 21**, deliberately kept as a single number in three places:

| Setting | Where | Value |
|---|---|---|
| Gradle daemon JVM | `gradle/gradle-daemon-jvm.properties` | 21 |
| Compile toolchain | `kotlin { jvmToolchain(21) }` in each module | 21 |
| Bytecode target | `compileOptions` in each module | 21 |

Why they must match: AGP checks at execution time that Kotlin's `jvmTarget` equals
`compileOptions.targetCompatibility` and fails the build with "Inconsistent JVM targets"
if they differ. Keeping the daemon on the same version too means Gradle never has to
provision a second JDK from `api.foojay.io` — a slow, network-dependent step that used to
run on every build here.

Why 21: it clears AGP 9's minimum of 17, it is what AGP 10 will require of the daemon
anyway, and D8 accepts Java 21 bytecode. This codebase is 100% Kotlin, so the bytecode
target only governs Kotlin's `jvmTarget`.

**Local setup.** Android Studio's bundled JBR is already 21, so IDE syncs and builds work
with no setup. Gradle does *not* auto-detect that JDK from a terminal, though, so if
`./gradlew` reports it cannot find a Java 21 toolchain, either install one:

```bash
brew install --cask temurin@21
```

or point Gradle at Android Studio's JBR by adding this to your **user** Gradle properties
(`~/.gradle/gradle.properties`, not this repo — the path is machine-specific):

```properties
org.gradle.java.installations.paths=/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

### CI

- `.github/workflows/android-ci.yml` — static analysis, unit tests + coverage, and
  assemble, all running in parallel on every push and pull request to `master`/`dev`.
- `.github/workflows/instrumentation.yml` — emulator tests, nightly and on demand.
  Kept off the pull request path because booting an emulator costs 10-15 minutes.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
