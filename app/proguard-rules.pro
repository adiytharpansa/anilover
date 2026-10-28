# REA — aturan R8 rilis
# Model data aplikasi (dibaca/ditulis lewat refleksi ringan & org.json manual)
-keep class com.app.rea.data.** { *; }
# Aktivitas yang dirujuk manifest
-keep class com.app.rea.MainActivity { *; }
-keep class com.app.rea.player.PlayerActivity { *; }
# Atribut anotasi untuk library Compose/Media3
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
