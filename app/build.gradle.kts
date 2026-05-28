plugins {
    alias(libs.plugins.android.application)
    id("com.diffplug.spotless")
}

// Conditionally apply Google Services plugin only if google-services.json exists
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.split_basket"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.split_basket"
        minSdk = 23
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        disable += "PropertyEscape"
    }
}

dependencies {

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // 添加Gson依赖
    implementation("com.google.code.gson:gson:2.13.2")
    // 添加Room依赖
    implementation("androidx.room:room-runtime:2.8.3")
    implementation(libs.room.external.antlr)
    implementation(libs.exifinterface)
    annotationProcessor("androidx.room:room-compiler:2.8.3")
    // 添加ML Kit Vision Label dependencies
    implementation("com.google.mlkit:image-labeling:17.0.7")
    // 添加TensorFlow Lite Interpreter dependencies
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    // 添加WorkManager依赖
    implementation(libs.work.runtime)
    // WorkManager依赖Guava的ListenableFuture（使用兼容Android的版本）
    implementation("com.google.guava:guava:32.0.1-android")
    // 添加Firebase依赖（使用BOM）
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

spotless {
    java {
        googleJavaFormat()
    }
}