# Release Notes — v1.0.0

First production release of **Video to Audio Converter**.

## What it does
Extract the audio track from a video on your device and save it as MP3, M4A,
WAV, or OGG. All processing happens **on your device** — your videos are never
uploaded.

## Features
- Pick a video from your device (MP4, MKV, MOV, AVI, and more).
- Convert to **MP3 / M4A / WAV / OGG**.
- Choose **bitrate**, **sample rate** (44.1 / 48 kHz), and **mono / stereo**.
- Optional **trim** to extract just the part you want.
- Built-in **audio player** with seek / play / pause / restart.
- **Open** or **share** the result to any app via a system share sheet.
- **Conversion history** with the ability to replay, open, share, or delete
  previous results.
- Dark / light / system **theme** following your preference.
- Lightweight **AdMob** banner + interstitial ads (interstitials only shown
  after a successful conversion, with frequency capping).

## Known limitations
- Conversion runs in the foreground; if the OS kills the app under memory
  pressure mid-conversion, that single conversion is lost (the app is not a
  foreground service).
- WAV output is uncompressed, so bitrate does not apply.

## Privacy
Media is processed on-device only and is never uploaded. Advertising is
provided by Google AdMob (see the in-app Privacy Policy and the Play Data
Safety form). No accounts, no analytics, no personal data collected.
