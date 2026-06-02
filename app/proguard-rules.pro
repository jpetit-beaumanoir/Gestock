# Reglas ProGuard. minifyEnabled está a false en release, así que por defecto no se aplica nada.
# Si en el futuro se activa la ofuscación, conservar los modelos usados por Gson/Retrofit:
-keep class com.beaumanoir.gestock.data.API.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
