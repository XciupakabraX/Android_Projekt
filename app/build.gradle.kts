plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.projekt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.projekt"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.common)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // Obsługa bazy danych Room
    implementation("androidx.room:room-runtime:2.5.0")
    annotationProcessor("androidx.room:room-compiler:2.5.0")

    // Retrofit do obsługi API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Picasso do ładowania obrazów
    implementation("com.squareup.picasso:picasso:2.8")

    // Obsługa sensora potrząśnięcia
    implementation("com.squareup:seismic:1.0.3")

    // Obsługa widoku RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("androidx.room:room-runtime:2.5.0")
    implementation ("com.google.android.material:material:1.10.0")


}