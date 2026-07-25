plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "es.guiamayores.fase0"
    compileSdk = 34

    defaultConfig {
        applicationId = "es.guiamayores.fase0"
        minSdk = 24          // Android 7 en adelante: cubre practicamente cualquier movil en uso
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-fase0"
    }

    buildTypes {
        // Solo compilamos la version de pruebas: se firma con la clave de
        // depuracion que genera Android sola, asi no hace falta gestionar
        // certificados de firma para instalarla en un movil propio.
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
