plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.akosidave.tlstunnel"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.akosidave.magiconboard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
