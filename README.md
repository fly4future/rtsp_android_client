# RTSP Client (Android / Kotlin)

Minimal app that plays an RTSP stream using **Media3/ExoPlayer** (`media3-exoplayer-rtsp` module) — the native, Google-recommended way to do RTSP on Android, without depending on third-party native libraries like VLC.

## Structure

```
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

```
rtsp://192.168.144.25:8554/main.264
```

On launch the app connects automatically to that URL; you can edit it and press "Connect" to point to another camera/server without recompiling. The default value is defined in `app/src/main/java/com/example/rtspclient/MainActivity.kt` (`defaultRtspUrl`) and in the layout `app/src/main/res/layout/activity_main.xml` (`android:text` of the `EditText`).

`setForceUseRtpTcp(true)` forces RTP over TCP instead of UDP — more reliable behind NAT/firewalls (typical on mobile networks or corporate wifi). If your RTSP server works fine over UDP and you want lower latency, you can remove that line.

## Building the APK with Docker (no Android Studio/SDK install needed)

Requirements: Docker only.

```bash
./build.sh
```

This will:
1. Build a Docker image based on `gradle:8.7.0-jdk17` with the Android SDK (platform 34 + build-tools 34.0.0) installed.
2. Run `gradle assembleDebug` inside the container.
3. Copy the resulting APK to `output/app-debug.apk`.

You can also do it manually:

```bash
docker build -t rtsp-android-builder .
docker create --name rtsp-tmp rtsp-android-builder
docker cp rtsp-tmp:/project/app/build/outputs/apk/debug/app-debug.apk ./output/app-debug.apk
docker rm rtsp-tmp
```

## Installing on a device/emulator

With `adb` connected (via USB or WiFi debugging):

```bash
adb install -r output/app-debug.apk
```

## Notes

- `minSdk = 24`, `compileSdk = targetSdk = 34`.
- The manifest includes `android:usesCleartextTraffic="true"` — needed to allow the stream's traffic if your RTSP server doesn't use encrypted transport.
- To build a signed **release** APK you'll need to configure your own keystore (not included here); `gradle assembleRelease` with a `signingConfig` in `app/build.gradle.kts`.
