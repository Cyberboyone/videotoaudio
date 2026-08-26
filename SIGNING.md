# Signing & CI setup (GitHub Actions)

This project builds a **signed** App Bundle (AAB) and APK via GitHub Actions.
No keystore or password is ever committed to the repository — everything is
provided through **GitHub Encrypted Secrets**.

## 1. Create a release keystore (locally, once)
```bash
keytool -genkey -v -keystore keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias videotoaudio
```
Remember the keystore password, the key password, and the alias
(`videotoaudio` above).

## 2. Encode the keystore as base64
- **Linux/macOS:**
  ```bash
  base64 -w0 keystore.jks > keystore.jks.b64
  ```
- **Windows (PowerShell):**
  ```powershell
  certutil -encode keystore.jks keystore.jks.b64
  ```
  Then open `keystore.jks.b64` and copy **only** the base64 body (between the
  `-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----` lines),
  without line breaks.

## 3. Add GitHub repository secrets
In **GitHub → repo → Settings → Secrets and variables → Actions**, add:

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | base64 body from step 2 |
| `SIGNING_KEY_ALIAS` | `videotoaudio` |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_PASSWORD` | key password |
| `ADMOB_APP_ID` | `ca-app-pub-9529770421530115~2406536983` |
| `ADMOB_BANNER_HOME` | `ca-app-pub-9529770421530115/6368998576` |
| `ADMOB_BANNER_HISTORY` | `ca-app-pub-9529770421530115/6368998576` |
| `ADMOB_INTERSTITIAL` | `ca-app-pub-9529770421530115/6716482939` |

> Production AdMob IDs are **not** hard-coded. The workflow writes
> `admob.properties` from these secrets at build time (and `admob.properties`
> is gitignored). Debug builds always use Google test ad IDs.

## 4. Build
Push a tag to trigger the workflow, or run it manually:
```bash
git tag v1.0.0
git push origin v1.0.0
```
The `Release Build (AAB + APK)` workflow will:
1. Set up JDK 17 + Android SDK (platform-34, build-tools 34.0.0).
2. Decode the keystore and create `admob.properties` from secrets.
3. Run `./gradlew :app:bundleRelease :app:assembleRelease` (R8 minified,
   signed).
4. Upload **AAB** and **APK** as downloadable artifacts.

## 5. Publish
- Download the AAB artifact and upload it to the **Google Play Console**
  (start with Internal Testing / closed track).
- Fill in the **Data Safety** form using `DATA_SAFETY.md`.
- Host `PRIVACY_POLICY.md` and set its URL in
  `res/values/strings.xml` (`privacy_policy_url`).

## Notes
- `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` are committed so CI can run
  the build reproducibly.
- `*.jks`, `*.keystore`, and `admob.properties` are gitignored — never commit
  them.
