plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    kotlin("kapt")
    id("com.jakewharton.butterknife")
}

android {
    namespace = "com.jgm90.cloudmusic"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.jgm90.cloudmusic"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-beta1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(project(":designsystem"))
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.fragment.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.6.2")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.afollestad.material-dialogs:core:0.9.6.0")
    implementation("org.greenrobot:eventbus:3.0.0")
    implementation("com.karumi:dexter:5.0.0")
    implementation("com.jakewharton:butterknife:10.2.3")
    kapt("com.jakewharton:butterknife-compiler:10.2.3")
    implementation("com.github.bumptech.glide:glide:4.10.0")
    kapt("com.github.bumptech.glide:compiler:4.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
