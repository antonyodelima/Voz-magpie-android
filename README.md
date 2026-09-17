# Vozo Magpie Studio (Android)

Native Android application rewritten with Kotlin and Jetpack Compose for **Vozo Magpie Studio / Voice Lab**.

## Features

- **Embedded Voice Studio**: High-performance hardware-accelerated WebView connecting directly to the Vozo Magpie Voice Lab suite (`https://vozo-voice-lab.base44.app/`).
- **Edge-to-Edge Experience**: Modern Material 3 dark-themed UI matching the signature Vozo aesthetic (`#0B0B10`, `#D946EF`, `#8B5CF6`).
- **Hardware Back Navigation**: Full Android system back-press integration to navigate history within the web lab seamlessly.
- **Microphone & Media Support**: Integrated runtime permission handling (`RECORD_AUDIO`, WebRTC audio capture) for voice recordings and audio synthesis.
- **Custom Loading Experience**: Animated pulsing waveform loading overlay with real-time progress indicators.
- **Offline Diagnostics & Recovery**: Elegant offline / error fallback screen with one-tap connection retries and external browser fallbacks.
- **Quick Toolbar**: One-tap controls for Back, Forward, Reload, Home, Share, and External Browser.

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Platform**: Android SDK 35+ (minSdk 24)
- **Build System**: Gradle 9.3.1 (Kotlin DSL)
