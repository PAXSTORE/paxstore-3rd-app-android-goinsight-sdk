# Proguard

    #Gson
    -dontwarn com.google.gson.**
    -keep class sun.misc.Unsafe { *; }
    -keep class com.google.gson.** { *; }
    -keep class com.google.gson.examples.android.model.** { *; }
    
    #JWT (nimbus-jose-jwt, used by paxstore-3rd-app-android-sdk 11.0.0+)
    -keepattributes InnerClasses
    -keep class org.bouncycastle.** { *; }
    -keepnames class org.bouncycastle.** { *; }
    -dontwarn org.bouncycastle.**
    -keep class com.nimbusds.** { *; }
    -dontwarn com.nimbusds.**
    -dontwarn javax.xml.bind.DatatypeConverter
    
    -dontwarn org.xml.sax.**
    -keep class org.xml.sax.**{*;}
    -dontwarn com.pax.market.api.sdk.java.base.util.**
    -keep class com.pax.market.api.sdk.java.base.util.**{*;}
    -dontwarn org.w3c.dom.**
    -keep class org.w3c.dom.**{*;}
    -dontwarn javax.xml.**
    -keep class javax.xml.**{*;}
    
    #dto
    -dontwarn com.pax.market.api.sdk.java.base.dto.**
    -keep class com.pax.market.api.sdk.java.base.dto.**{*;}
    
    #By default Google keeps classes of  Activity 、Service ... 
    -keep public class * extends android.app.Activity
    -keep public class * extends android.app.Service
    -keep public class * extends android.content.BroadcastReceiver
    -keep public class * extends android.preference.Preference

## 