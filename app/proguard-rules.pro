# Aseman R8 / ProGuard rules

# Retrofit service interfaces
-keep interface com.iliyateam.aseman.data.WeatherApi { *; }
-keep interface com.iliyateam.aseman.data.AirApi { *; }

# Gson / Retrofit annotations
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault,Signature

# Keep Gson model fields
-keepclassmembers class com.iliyateam.aseman.data.** {
    <fields>;
}

# Android entry points
-keep class com.iliyateam.aseman.MainActivity { *; }
-keep class com.iliyateam.aseman.WeatherWorker { *; }
-keep class com.iliyateam.aseman.RefreshService { *; }
-keep class com.iliyateam.aseman.BootReceiver { *; }
-keep class com.iliyateam.aseman.WeatherWidgetProvider { *; }

# Keep Application class
-keep class com.iliyateam.aseman.AsemanApp { *; }