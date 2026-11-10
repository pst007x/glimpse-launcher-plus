import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.glimpsemedia.glimpselauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.glimpsemedia.glimpselauncher"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "1.0.14"
        manifestPlaceholders["appLabel"] = "Glimpse Launcher"
    }

    // The upload key lives outside the project (see docs/release/README.md); without it the release is unsigned.
    val keystoreFile = file(System.getProperty("user.home") + "/glimpse-launcher-keys/keystore.properties")
    val keystoreProps = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }
    // Glimpse Launcher Plus has its own key, next to the Play one; without it the Plus release is unsigned.
    val plusKeystoreFile = file(System.getProperty("user.home") + "/glimpse-launcher-keys/plus-keystore.properties")
    val plusKeystoreProps = Properties().apply { if (plusKeystoreFile.exists()) plusKeystoreFile.inputStream().use { load(it) } }
    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
        if (plusKeystoreFile.exists()) {
            create("plus") {
                storeFile = file(plusKeystoreProps.getProperty("storeFile"))
                storePassword = plusKeystoreProps.getProperty("storePassword")
                keyAlias = plusKeystoreProps.getProperty("keyAlias")
                keyPassword = plusKeystoreProps.getProperty("keyPassword")
            }
        }
    }

    // Two editions from one project: Play (on Google Play, unchanged) and Plus (outside Play, with extras Play would
    // not take in a launcher, such as searching every file name). Plus-only code lives in src/plus, its stand-ins in src/play.
    flavorDimensions += "edition"
    productFlavors {
        // Each edition's release is signed with its own key (debug and preview builds keep the debug key).
        create("play") {
            dimension = "edition"
            signingConfigs.findByName("release")?.let { signingConfig = it }
            buildConfigField("boolean", "PLUS", "false")
        }
        create("plus") {
            dimension = "edition"
            applicationId = "com.glimpsemedia.glimpselauncherplus"
            signingConfigs.findByName("plus")?.let { signingConfig = it }
            manifestPlaceholders["appLabel"] = "Glimpse Launcher Plus"
            buildConfigField("boolean", "PLUS", "true")
        }
    }

    buildTypes {
        // A second copy with its own data and name, for trying things on the phone without touching the real one.
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".preview"
            manifestPlaceholders["appLabel"] = "Glimpse test"
            matchingFallbacks += "debug"
        }
        release {
            // For trying the release build next to the real one: -PidSuffix=.rctest
            (project.findProperty("idSuffix") as String?)?.let { applicationIdSuffix = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

androidComponents {
    onVariants { variant ->
        val plus = variant.productFlavors.any { it.second == "plus" }
        if (variant.buildType == "preview") variant.manifestPlaceholders.put("appLabel", if (plus) "Glimpse Plus test" else "Glimpse test")
    }
}

// The Play edition must never ask for the permissions only Plus has: storage, and installing apps.
val plusOnlyPermissions = listOf("MANAGE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE", "REQUEST_INSTALL_PACKAGES")
tasks.matching { it.name == "processPlayReleaseManifest" }.configureEach {
    doLast {
        outputs.files.asFileTree.matching { include("**/AndroidManifest.xml") }.forEach { f ->
            val t = f.readText()
            plusOnlyPermissions.firstOrNull { it in t }?.let { throw GradleException("Play manifest has a Plus-only permission ($it): $f") }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
}
