# Google Play "Data Safety" — disclosure to enter in Play Console

Enter the following in **Play Console → App content → Data safety**.

## Does your app collect or share any of the required user data?
**Yes**

## Types of user data collected (app interacts with / collects)

| Data type | Collected? | Shared? | Purpose | Required / Optional |
|---|---|---|---|---|
| Advertising ID | Yes | Yes (with Google/AdMob) | Ads delivery, measurement, fraud prevention | Optional (can be reset/opt-out) |
| Approximate location (IP-derived) | Yes | Yes (with Google/AdMob) | Region-appropriate ads | Optional |
| App interactions / ad interactions | Yes | Yes (with Google/AdMob) | Ad performance / measurement | Optional |
| Device or other IDs (Advertising ID) | Yes | Yes (with Google/AdMob) | Ads | Optional |
| Diagnostics (crash/performance) | Yes | Yes (with Google/AdMob) | Service improvement | Optional |

> **Media / files (video & audio):** These are processed **on-device only**
> and are **NOT** collected or transmitted to the developer or any third party.
> When asked "Is this data collected or shared?" for *media / files*, answer
> **"No"** (the App does not upload them).

## Does your app collect or share any precise location?
**No** (only approximate, IP-derived location via ads).

## Does your app collect or share any financial data?
**No**

## Does your app collect or share any health & fitness data?
**No**

## Does your app collect or share any personal info (name, email, etc.)?
**No**

## Is all user data collected by the app encrypted in transit?
**Yes** (ad traffic uses HTTPS; AdMob requires TLS).

## Do you provide a way for users to request deletion of their data?
Yes — users can delete generated audio and history inside the App, and
uninstalling removes all local data. Note this data never leaves the device,
so there is no server-side data to delete.

## Data safety summary text (for the store listing)
"Video to Audio Converter processes your selected videos and extracted audio
entirely on your device and does not upload them. The only third-party data
sharing is with Google AdMob for advertising (Advertising ID, approximate
location from IP, ad interactions), subject to Google's privacy policy."
