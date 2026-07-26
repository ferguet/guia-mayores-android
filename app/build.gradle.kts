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

    // Llave de firma FIJA, guardada en el propio proyecto.
    //
    // Sin esto, cada compilacion en GitHub genera una llave nueva, y Android
    // se niega a sustituir una app por otra firmada distinto: sale el error
    // "conflicto de paquete" y hay que desinstalar a mano cada vez. Con una
    // llave fija, las actualizaciones se instalan encima sin mas.
    //
    // Es la llave de DEPURACION estandar de Android (contraseña "android",
    // conocida por todo el mundo). No protege nada y no es un secreto: si
    // algun dia se publica en Google Play hara falta una llave de verdad,
    // distinta y guardada aparte.
    signingConfigs {
        create("fija") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fija")
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
