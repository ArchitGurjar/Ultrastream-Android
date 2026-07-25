-keep class com.ultrastream.app.data.models.** { *; }
-keep class com.ultrastream.app.network.** { *; }

-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep @com.squareup.moshi.JsonClass class *

-keep class dagger.hilt.** { *; }

-keep class androidx.media3.** { *; }
