plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.carvision.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carvision.ai"
        buildConfigField("String", "CARVISION_AI_URL", "\"${project.findProperty("CARVISION_AI_URL") ?: ""}\"")
        buildConfigField("String", "CARVISION_AI_KEY", "\"${project.findProperty("CARVISION_AI_KEY") ?: ""}\"")
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // On-device object/image labeling for a fast car check.
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Networking for the optional Gemini vision call.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
}
