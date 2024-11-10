// App-level build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.remind"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.remind"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "another/path")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    //Gemini dependencies
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")


    // Retrofit dependencies
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // UI and Material3 for Compose
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation(libs.vision.common)
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.ui:ui-text")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth-ktx:<latest_version>")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.firebase:firebase-database-ktx")
    implementation ("io.coil-kt:coil-compose:2.5.0")
    // Compose dependencies
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.foundation:foundation:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    implementation("androidx.compose.material:material-icons-extended:1.4.3")
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("com.google.mlkit:common:17.0.1") // Common dependency for ML Kit

    // Additional dependencies
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.mlkit:face-detection:16.0.3")
    implementation ("com.google.android.gms:play-services-mlkit-face-detection:16.1.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
    implementation ("androidx.camera:camera-core:1.1.0")
    implementation ("org.tensorflow:tensorflow-lite:2.10.0")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.10.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.0")
    implementation ("androidx.camera:camera-camera2:1.1.0")
    implementation ("androidx.camera:camera-lifecycle:1.1.0")
    implementation ("androidx.camera:camera-view:1.1.0")
    implementation ("androidx.camera:camera-extensions:1.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")
    implementation ("androidx.camera:camera-core:1.1.0")
    implementation ("androidx.camera:camera-camera2:1.1.0")
    implementation ("androidx.camera:camera-lifecycle:1.1.0")
    implementation ("androidx.camera:camera-view:1.0.0-alpha31")
    implementation ("androidx.compose.ui:ui:1.5.1")
    implementation ("androidx.compose.ui:ui-text:1.7.4")
    implementation ("androidx.compose.material3:material3:1.2.0")
    implementation ("androidx.compose.foundation:foundation:1.5.1")
    implementation ("androidx.compose.ui:ui:1.0.1")
    implementation ("androidx.compose.material3:material3:1.0.0-alpha01")
    implementation ("androidx.activity:activity-compose:1.3.1")
    implementation("org.tensorflow:tensorflow-lite:2.9.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.3.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")


    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
apply(plugin = "com.google.gms.google-services")

