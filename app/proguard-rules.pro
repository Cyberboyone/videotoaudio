# ---------------------------------------------------------------------------
# ProGuard / R8 rules for Video to Audio Converter
# ---------------------------------------------------------------------------

# FFmpegKit talks to native code via JNI and may reference classes/methods that
# R8 cannot see as used. Keep the whole package to avoid NoSuchMethodError at
# runtime (e.g. Statistics.getTime()).
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.**

# Keep our domain/models that travel across process/serialization boundaries
# (Room entities, DataStore-backed settings, conversion request/result models)
# so their names/members are not renamed or removed.
-keep class com.nakudin.videotoaudio.model.** { *; }
-keep class com.nakudin.videotoaudio.domain.** { *; }
-keep class com.nakudin.videotoaudio.data.repository.** { *; }

# AdMob / Google Play Services ship their own consumer ProGuard rules; just
# silence any unrelated warnings so the release build stays warning-clean.
-dontwarn com.google.android.gms.**
-dontwarn com.google.ads.**

# General safety net for coroutines / annotations / signatures.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
