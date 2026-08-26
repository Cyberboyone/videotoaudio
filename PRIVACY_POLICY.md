# Privacy Policy — Video to Audio Converter

**Last updated:** 2026-08-26

Video to Audio Converter ("the App") converts video files stored on your device
into audio files (MP3, M4A, WAV). This policy explains what data the App
handles and why.

## 1. Data we process on your device
- **Video files you select.** When you choose a video, the App reads it and
  extracts its audio track using Android's built-in media engine
  (MediaCodec / MediaExtractor / MediaMuxer). **The original video is
  never uploaded** and is never modified.
- **Generated audio files.** The extracted audio is written to the App's
  private storage folder (`Android/data/com.nakudin.videotoaudio/...`). These
  files stay on your device and are removed when you delete them in the App or
  uninstall the App.
- **Conversion history.** The App keeps a local history (file name, format,
  size, duration, date) of conversions in on-device storage (Room database) so
  you can revisit previous results. This history is **not** transmitted
  anywhere.

## 2. Data we do NOT collect
We do **not** collect your name, email, account information, contacts,
location, photos, or the contents of your media. We do **not** operate user
accounts and we do **not** upload your videos or audio.

## 3. Advertising (Google AdMob)
The App is supported by Google AdMob, which may show banner and interstitial
ads. To deliver ads, AdMob may collect and process:
- **Advertising ID** (resettable, used for ads measurement and
  personalization),
- **Device information** (model, OS version),
- **Approximate location** derived from IP address (used only for
  region-appropriate ads),
- **IP address** and **ad interaction data**.

AdMob may share this data with Google and, where you have consented, with
advertising partners, as described in
[Google's privacy policy](https://policies.google.com/privacy) and
[Google's ads policy](https://policies.google.com/technologies/ads).

You can reset or opt out of ads personalization in your device's
*Settings → Google → Ads* (Reset advertising ID / Opt out of personalized ads).
Where required (e.g. EEA), ads consent is handled by Google's UMP/consent
platform.

## 4. Children
The App is not directed to children under 13 and does not knowingly collect
data from children.

## 5. Your choices
- Delete any generated audio file and its history entry from inside the App.
- Uninstall the App to remove all locally stored data.
- Reset/opt out of the Advertising ID in device settings.

## 6. Data retention
All data processed by the App stays on your device and is removed when you
delete it or uninstall the App. Advertising data is governed by Google's
retention practices.

## 7. Contact
For privacy questions, contact the developer through the Google Play store
listing.

---

*This document should be hosted (e.g. on GitHub Pages or your own site) and its
URL entered in the Google Play Console and in
`res/values/strings.xml` (`privacy_policy_url`). The App also contains an
in-app copy of this policy under Settings → Privacy Policy.*
