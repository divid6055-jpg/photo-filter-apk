import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// قراءة إعدادات التوقيع من keystore.properties (لو موجود)
val keystorePropertiesFile = File(rootDir, "keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.photofilter.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.photofilter.pro"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // يعمل محلياً (من keystore.properties) وفي CI (من environment variables)
            val storeFilePath = System.getenv("SIGNING_KEYSTORE_FILE")
            val storeBase64 = System.getenv("SIGNING_KEYSTORE_BASE64")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = File(storeFilePath)
            } else if (!storeBase64.isNullOrBlank()) {
                // فك base64 واكتب لملف مؤقت
                val tmpFile = File.createTempFile("release-keystore", ".jks")
                tmpFile.writeBytes(java.util.Base64.getDecoder().decode(storeBase64))
                storeFile = tmpFile
            } else if (keystoreProperties.containsKey("storeFile")) {
                storeFile = File(keystoreProperties.getProperty("storeFile"))
            }
            storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("storePassword") ?: ""
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: keystoreProperties.getProperty("keyAlias") ?: ""
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: keystoreProperties.getProperty("keyPassword") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // توقيع الـ release APK لو توفّرت المفاتيح
            val hasSigning = (System.getenv("SIGNING_KEYSTORE_BASE64") != null ||
                    System.getenv("SIGNING_KEYSTORE_FILE") != null ||
                    keystoreProperties.containsKey("storeFile"))
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Material Design 3
    implementation("com.google.android.material:material:1.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Image loading
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
