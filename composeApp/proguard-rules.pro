# Reglas de R8 para la release de Murmr.
# Solo incluimos lo que viene de uso reflexivo o de carga JNI:
#  - kotlinx.serialization usa @Serializable y el companion KSerializer<T>.
#  - Koin escanea los constructores en runtime.
#  - Voyager mueve ScreenModel y Screen por reflexión del classpath.
#  - SQLDelight genera adapters concretos pero accede a sus interfaces por nombre.
#  - El bridge JNI de ggwave carga símbolos nativos por nombre.
#  - Las funciones @Composable las preserva la regla por defecto del plugin
#    de Android; aquí mantenemos las anotaciones por si las usa alguna herramienta.

# kotlinx.serialization: companion KSerializer y clases @Serializable.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    public static <1>$Companion access$getCompanion$cp();
}
-if @kotlinx.serialization.Serializable class **
-keep class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class uoc.edu.psirvent.murmr.**$$serializer { *; }
-keepclassmembers class uoc.edu.psirvent.murmr.** {
    *** Companion;
}
-keep class uoc.edu.psirvent.murmr.data.network.** { *; }

# Koin DI: necesita conservar constructores y nombres simples de los módulos.
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <init>(...);
}

# Voyager: ScreenModel y Screen se cargan reflexivamente desde la jerarquía de Composables.
-keep class cafe.adriel.voyager.** { *; }
-keep class * extends cafe.adriel.voyager.core.screen.Screen { *; }
-keep class * extends cafe.adriel.voyager.core.model.ScreenModel { *; }

# SQLDelight: adapters generados acceden a Driver y SqlSchema por interfaz.
-keep class app.cash.sqldelight.** { *; }
-keep class uoc.edu.psirvent.murmr.db.** { *; }

# Bridge JNI de ggwave: el códec carga símbolos nativos por nombre.
-keep class io.github.tfg_uoc.ggwave.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Coroutines: campos usados por reflexión en Continuation y BaseContinuationImpl.
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Anotaciones de Compose: el plugin ya preserva las funciones, pero conviene
# mantener las anotaciones por si las inspecciona alguna herramienta externa.
-keep,allowshrinking @interface androidx.compose.runtime.Composable
-keep,allowshrinking @interface androidx.compose.runtime.Stable
-keep,allowshrinking @interface androidx.compose.runtime.Immutable

# Stack traces legibles tras un crash: conservamos los nombres de fichero y líneas.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# Anotaciones de errorprone que Tink (a través de security-crypto-ktx)
# referencia pero que nunca están en runtime. Sin esto, R8 falla.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Tink usa reflexión para los KeyTemplate; mantenemos toda su API pública.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
