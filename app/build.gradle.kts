import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.techenglish.app"
    compileSdk = 35

    val requestedTasks = gradle.startParameter.taskNames
    val isStandaloneOnly = requestedTasks.isNotEmpty() &&
        requestedTasks.any { it.contains("Standalone", ignoreCase = true) } &&
        requestedTasks.none { it.contains("Pad", ignoreCase = true) }
    if (!isStandaloneOnly) {
        assetPacks += listOf(":content_pack")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("pad") {
            dimension = "distribution"
        }
        create("standalone") {
            dimension = "distribution"
        }
    }

    sourceSets {
        getByName("debug") {
            assets.srcDir(rootProject.file("content_pack/src/main/assets"))
        }
        getByName("test") {
            assets.srcDir(rootProject.file("content_pack/src/main/assets"))
        }
        getByName("standalone") {
            assets.srcDir(rootProject.file("content_pack/src/main/assets"))
        }
    }

    defaultConfig {
        applicationId = "com.techenglish.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 320
        versionName = "3.2.0"

        val crashReportingEnabled = (System.getenv("CRASH_REPORTING_ENABLED") ?: "false").toBoolean()
        val sentryDsn = (System.getenv("SENTRY_DSN") ?: "").replace("\"", "\\\"")
        val tajikUiEnabled = (System.getenv("TAJIK_UI_LOCALIZATION_ENABLED") ?: "false").toBoolean()
        val apiBaseUrl = (project.findProperty("API_BASE_URL")?.toString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("API_BASE_URL")?.takeIf { it.isNotBlank() }
            ?: "https://techenglish-trwk.onrender.com").trim().trimEnd('/').replace("\"", "\\\"")
        val otaManifestFallbackUrl = (project.findProperty("OTA_MANIFEST_FALLBACK_URL")?.toString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("OTA_MANIFEST_FALLBACK_URL")?.takeIf { it.isNotBlank() }
            ?: "https://raw.githubusercontent.com/tuychievmt/techenglish/main/ota/latest.json").trim().replace("\"", "\\\"")
        buildConfigField("boolean", "CRASH_REPORTING_ENABLED", crashReportingEnabled.toString())
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
        buildConfigField("boolean", "TAJIK_UI_LOCALIZATION_ENABLED", tajikUiEnabled.toString())
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "OTA_MANIFEST_FALLBACK_URL", "\"$otaManifestFallbackUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val keystoreProps = Properties().apply {
        val candidates = listOf(
            rootProject.file("keystore/keystore.properties"),
            rootProject.file("keystore.properties")
        )
        candidates.firstOrNull { it.isFile }?.inputStream()?.use { load(it) }
    }
    fun signingProp(envKey: String, propKey: String): String? =
        System.getenv(envKey)?.takeIf { it.isNotBlank() }
            ?: project.findProperty(envKey)?.toString()?.takeIf { it.isNotBlank() }
            ?: keystoreProps.getProperty(propKey)?.trim()?.takeIf { it.isNotBlank() }

    val releaseKeystorePath = signingProp("KEYSTORE_PATH", "storeFile")
    val releaseKeystoreFile = releaseKeystorePath?.let { path ->
        val f = file(path)
        if (f.isAbsolute) f else rootProject.file(path)
    }
    val releaseKeystorePassword = signingProp("KEYSTORE_PASSWORD", "storePassword")
    val releaseKeyAlias = signingProp("KEY_ALIAS", "keyAlias")
    val releaseKeyPassword = signingProp("KEY_PASSWORD", "keyPassword")
    val hasReleaseSigning = releaseKeystoreFile != null &&
        releaseKeystoreFile.exists() &&
        releaseKeystorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile!!
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
            isReturnDefaultValues = true
            all {
                it.maxHeapSize = "1536m"
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}

dependencies {
    implementation(project(":domain"))

    // Core & Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    // Lifecycle & Activity
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)

    // Glance Home Screen Widget (ФТ-7.4)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Network (Ktor Client — §6.1, single stack, no Retrofit) & Coil avatars
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.coil.compose)

    // Testing (Unit + Robolectric Room & Compose UI tests)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit.ktx)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Runs pad debug unit tests (backward-compatible alias after adding distribution flavors)."
    dependsOn("testPadDebugUnitTest")
}
