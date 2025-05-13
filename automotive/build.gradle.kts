plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.lindehammarkonsult.automus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lindehammarkonsult.automus"
        minSdk = 31
        targetSdk = 35
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
    
    buildFeatures {
        viewBinding = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(project(":shared"))
    // Removed security-crypto dependency as it's no longer needed
    // Automotive
    implementation(libs.androidx.app)
    implementation(libs.androidx.app.automotive)
    
    // UI
    implementation(libs.material)
    
    // Image Loading
    implementation(libs.glide)
    implementation(libs.androidx.media3.common)
    annotationProcessor(libs.glide.compiler)
    
    // UI Layout
    implementation(libs.flexbox)
    
    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.livedata)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}