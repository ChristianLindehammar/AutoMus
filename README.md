# AutoMus

## Project Description

AutoMus is an Apple Music app designed for Android Automotive OS using Kotlin. The app provides a seamless music experience for users while driving, with features such as music playback, queue management, music discovery, library access, and voice support.

## Core Functional Requirements

### Music Playback
- Play, pause, skip, repeat, shuffle Apple Music tracks
- Queue management: View and modify current playback queue
- Now Playing screen with track title, artist, album art, and playback controls
- Audio focus handling: Respect Android Automotive OS standards for handling focus and audio interruptions (e.g., navigation prompts)

### Music Discovery & Search
- Search Apple Music library by song, album, artist, playlist, or genre
- Browse curated content: Access Apple Music’s top charts, new releases, and editorial playlists
- Category browsing: By genre, mood, activity, etc.

### Library Access
- Access user’s Apple Music library (including:
  - Playlists (created and saved)
  - Liked or loved songs
  - Recently played
  - Followed artists and albums

### Voice Support (if supported by car system)
- Support Google Assistant or built-in voice command for playback (e.g., “Play Taylor Swift on Apple Music”)

## Additional / Unique Requirements

### Apple Music Integration
- Apple Music API Usage: Use Apple MusicKit for Web (since no native Android SDK is provided by Apple)
- User Authentication via Apple ID & Apple Music subscription check
- Playback Authorization: Likely using a web-based player via secure token
- Music Playback may require:
  - AirPlay fallback (less ideal) or
  - Creative use of MusicKit JS in a WebView for authorized playback
  - Or using Apple Music on Android’s local app if Apple provides any hooks (which is unlikely)

### Authentication
- OAuth-style Apple ID login flow with secure token handling
- Securely store and refresh tokens using Android Keystore

## Usability & UX Best Practices (for In-Car Use)
- Follow Google’s Android Automotive UX Guidelines:
  - Large, high-contrast tap targets (minimum 48x48dp)
  - Minimal driver distraction: No text input while driving
  - Avoid complex interactions (e.g., scrolling long lists)
  - Support for day/night modes (light and dark themes)
  - Clear “Now Playing” feedback visible at all times
  - Touch interaction optimized for glove-friendly UIs
  - Support rotary controllers or D-pads (for vehicles with physical input)
  - Accessibility
  - Voice support where available
  - Descriptive content labels for screen readers

## Technical & Platform-Specific Requirements

### Android Automotive OS-Specific
- Build for Android Automotive OS (AAOS), not just Android Auto (embedded vs projection)
- Support for car hardware abstraction layer (Car HAL) if needed
- MediaBrowserService and MediaSession integration
- Use ExoPlayer (if handling playback natively)

### Offline & Connectivity
- Detect poor or no internet connection gracefully
- Fallback UI for offline state (e.g., downloaded playlists or warning)

## Setting Up the Project

1. Clone the repository:
   ```
   git clone https://github.com/ChristianLindehammar/AutoMus.git
   cd AutoMus
   ```

2. Open the project in Android Studio.

3. Build the project:
   ```
   ./gradlew build
   ```

4. Run the app on an Android Automotive OS emulator or a compatible device.

## Apple Music API Integration and Authentication

### Apple Music API Usage
- Use Apple MusicKit for Web to interact with the Apple Music API.
- Implement methods for searching the Apple Music library, retrieving user playlists, and accessing curated content.

### User Authentication
- Implement OAuth-style Apple ID login flow.
- Securely store and refresh tokens using Android Keystore.

### Playback Authorization
- Use a web-based player via secure token for music playback.
- Consider fallback options such as AirPlay or using MusicKit JS in a WebView.
