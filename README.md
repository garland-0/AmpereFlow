# AmpereFlow

A native Android battery-monitoring app: live percentage gauge, voltage, current,
wattage, temperature, health, plugged source, estimated max capacity, and a
foreground-service "Turn on monitoring" toggle with a persistent notification.

All numbers come from Android's real `BatteryManager` APIs (not simulated).

## Why this can't be compiled here

Building an `.apk` requires the Android SDK + Gradle, which needs internet access
to download — my sandbox has no network access, and installing the full Android
SDK on-device (even in Termux) is a heavy, fragile process. Instead, this project
is wired up to build itself automatically on **GitHub's free servers**, using the
included `.github/workflows/android-build.yml`. You never need Android Studio,
a PC, or to install the SDK yourself.

## How to get your APK (phone-only, ~10 minutes)

1. **Create a free GitHub account** if you don't have one (github.com, via Chrome).
2. **Create a new repository** — name it `AmpereFlow`, keep it public or private,
   don't initialize with a README (we already have one).
3. **Upload these files** to the repo. Easiest ways on Android:
   - GitHub's web uploader: open the repo → "Add file" → "Upload files" → select
     everything from this project (drag the whole extracted folder in via Chrome's
     file picker, or upload the zip and let GitHub unpack — if it won't unpack
     automatically, use Termux, see below).
   - Or, in Termux (you already use this for your WordPress work):
     ```
     pkg install git
     cd AmpereFlow
     git init
     git add .
     git commit -m "Initial commit"
     git branch -M main
     git remote add origin https://github.com/YOUR_USERNAME/AmpereFlow.git
     git push -u origin main
     ```
     (It'll prompt for a GitHub username + a Personal Access Token as the password —
     generate one at github.com → Settings → Developer settings → Personal access
     tokens, with "repo" scope.)
4. **Go to the "Actions" tab** on your GitHub repo. A workflow run should start
   automatically (or tap "Run workflow" if not). It takes 2-4 minutes.
5. When it finishes, open the completed run → **Artifacts** section at the bottom →
   download `AmpereFlow-debug-apk` (a zip containing `app-debug.apk`).
6. On your phone, open that zip (Files app), extract `app-debug.apk`, tap it to
   install. You'll need to allow "install unknown apps" for Chrome/Files when prompted
   — this is normal for any APK not from the Play Store.

## After installing

- Grant the notification permission when prompted if you tap "Turn on monitoring"
  (Android 13+) — this lets the persistent monitoring notification show.
- Some phones (Xiaomi/MIUI, Samsung, Oppo, etc.) aggressively kill background
  services. If monitoring stops working after a while, look for "Autostart" /
  "Battery optimization" settings for AmpereFlow and allow it to run unrestricted.

## Notes on the data

- Voltage, current, temperature, health, and plugged source come directly from
  Android's `BatteryManager` / `ACTION_BATTERY_CHANGED` broadcast — accurate on
  virtually all devices.
- "Max Capacity" tries a hidden system API (`PowerProfile.getBatteryCapacity`)
  that most OEMs support but isn't officially guaranteed; if unavailable, the app
  falls back to an estimate derived from the charge counter.
- Current sign convention (+/-) can vary slightly by OEM kernel; the app assumes
  positive = charging, which matches most devices including the reference screenshot.

## Project structure

```
AmpereFlow/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/darth/ampereflow/
│       │   ├── MainActivity.kt          — UI + battery polling
│       │   ├── BatteryMonitorService.kt — foreground monitoring service
│       │   └── CircularGaugeView.kt     — custom arc gauge
│       └── res/                          — layouts, colors, drawables
├── .github/workflows/android-build.yml   — auto-builds the APK on push
├── build.gradle / settings.gradle / gradle.properties
└── README.md
```
