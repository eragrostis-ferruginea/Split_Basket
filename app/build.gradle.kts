plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    id("com.diffplug.spotless")
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

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
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