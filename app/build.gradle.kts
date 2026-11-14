import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.novafiles.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.novafiles.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "2.0.0-alpha.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                load(stream)
            }
        }
    }

    val releaseStorePath = System.getenv("KEYSTORE_PATH") ?: localProperties.getProperty("KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("STORE_PASSWORD") ?: localProperties.getProperty("STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("KEY_ALIAS") ?: localProperties.getProperty("KEY_ALIAS")
    val releaseKeyPassword = System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("KEY_PASSWORD")

    signingConfigs {
        create("release") {
            val isConfigured = !releaseStorePath.isNullOrBlank() &&
                !releaseStorePassword.isNullOrBlank() &&
                !releaseKeyAlias.isNullOrBlank() &&
                !releaseKeyPassword.isNullOrBlank()

            if (isConfigured) {
                val rawFile = File(releaseStorePath)
                val ksFile = if (rawFile.isAbsolute) rawFile else rootProject.file(releaseStorePath)
                if (!ksFile.exists()) {
                    throw GradleException("Release keystore not found at: ${ksFile.absolutePath}")
                }
                storeFile = ksFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            } else if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
                throw GradleException("Release signing is not configured. Set KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:storage"))
    implementation(project(":data:files"))
    implementation(project(":feature:files"))
    implementation(project(":feature:dualpane"))
    implementation(project(":core:media"))
    implementation(project(":feature:music"))
    implementation(project(":feature:video"))
    implementation(project(":feature:gallery"))
    implementation(project(":core:security"))
    implementation(project(":feature:analyzer"))
    implementation(project(":feature:cleaner"))
    implementation(project(":feature:ftp"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
