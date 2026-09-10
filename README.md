# RTSP Client (Android / Kotlin)

Minimal app that plays an RTSP stream using **Media3/ExoPlayer** (`media3-exoplayer-rtsp` module) — the native, Google-recommended way to do RTSP on Android, without depending on third-party native libraries like VLC.

## Structure

```sh
rtsp-android-client/
├── app/
│   └── src/main/
│       ├── java/com/example/rtspclient/MainActivity.kt   # player logic
│       ├── res/layout/activity_main.xml                  # URL input + PlayerView
│       └── AndroidManifest.xml
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile          # builds the APK without Android Studio
└── build.sh            # helper: docker build + extract the APK
```

## Setting your RTSP URL

The URL is an editable field inside the app (`EditText` at the top + "Connect" button). It comes preloaded with:

```sh
rtsp://192.168.144.25:8554/main.264 # Default Siyi A8 mini RTSP server
```

> [!NOTE]
> On launch the app connects automatically to that URL; you can edit it and press "Connect" to point to another camera/server without recompiling.
>
> `setForceUseRtpTcp(true)` forces RTP over TCP instead of UDP — more reliable behind NAT/firewalls (typical on mobile networks or corporate wifi). If your RTSP server works fine over UDP and you want lower latency, you can remove that line.

## Building the APK with Docker (no Android Studio/SDK install needed)

Requirements: Docker only.

```bash
./build.sh
```

This will:

1. Build a Docker image based on `gradle:8.7.0-jdk17` with the Android SDK (platform 34 + build-tools 34.0.0) installed.
2. Run `gradle assembleDebug` inside the container.
3. Copy the resulting APK to `output/app-debug.apk`.

## Create an RTSP Server for Testing

To create a local RTSP server for testing, install the following tools:

- [MediaMTX](https://github.com/bluenviron/mediamtx/releases)
- FFmpeg(`sudo apt install ffmpeg` on Ubuntu/Debian)

### 1. Start MediaMTX

Download and extract MediaMTX, then start the server from its installation directory:

```bash
./mediamtx
```

By default, MediaMTX listens for RTSP connections on port `8554`.

### 2. Stream a Video with FFmpeg

Open a second terminal in the directory containing `loop.mp4` and run:

```bash
ffmpeg -re -stream_loop -1 -i loop.mp4 \
  -c copy \
  -f rtsp \
  rtsp://localhost:8554/test
```

This command:

- Reads `loop.mp4` at its native playback speed.
- Repeats the video indefinitely with `-stream_loop -1`.
- Copies the existing audio and video codecs without re-encoding.
- Publishes the stream at the `/test` path.

The resulting RTSP stream is available at:

```text
rtsp://localhost:8554/test
```
