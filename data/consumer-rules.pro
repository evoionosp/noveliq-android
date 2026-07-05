# Consumer ProGuard/R8 rules for the :data module. These are applied automatically to any app
# module that depends on :data (e.g. :app in release builds with minify enabled).

# --- Gson ---
# Gson serializes/deserializes the remote DTOs via reflection, so their classes and fields must
# not be removed or renamed. Retrofit/OkHttp/Room ship their own rules.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod

# Keep all remote DTOs (request/response models parsed by Gson).
-keep class org.evoionosp.noveliq.data.**.dto.** { *; }

# Keep any @SerializedName-annotated members even if their enclosing class is obfuscated.
-keepclassmembers,allowobfuscation class org.evoionosp.noveliq.data.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson uses TypeToken generic reflection; keep its signature.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
