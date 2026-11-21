# PDFBox
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
