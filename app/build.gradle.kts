import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// A chave que assina o aplicativo fica fora do repositório. Sem ela, o build de release
// sai assinado com a chave de testes, que serve para experimentar mas não para publicar.
val assinatura = Properties().apply {
    val arquivo = rootProject.file("keystore.properties")
    if (arquivo.exists()) arquivo.inputStream().use(::load)
}

android {
    namespace = "app.valeacorrida"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.valeacorrida"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1"
    }

    signingConfigs {
        if (assinatura.containsKey("arquivo")) {
            create("release") {
                storeFile = file(assinatura.getProperty("arquivo"))
                storePassword = assinatura.getProperty("senha")
                keyAlias = assinatura.getProperty("apelido")
                keyPassword = assinatura.getProperty("senha")
            }
        }
    }

    buildTypes {
        release {
            // Sem encolher o código por enquanto: assim o APK publicado é o mesmo código que os
            // testes exercitam. Ligar quando houver teste rodando o APK final num aparelho.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            // Só os processadores de celular. Os de computador (x86) servem a emulador e dobram o tamanho.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.maxHeapSize = "1536m" }
        }
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Os testes do aplicativo rodam na variante de teste (debug), que tem a tela vazia usada para
// desenhar as telas. Na de release eles não teriam onde rodar.
androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enableUnitTest = false }
}

dependencies {
    implementation(project(":nucleo"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.mlkit.texto)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
