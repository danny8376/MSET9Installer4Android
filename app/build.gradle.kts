plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "moe.saru.homebrew.console3ds.mset9_installer_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "moe.saru.homebrew.console3ds.mset9_installer_android"
        minSdk = 21
        targetSdk = 33
        versionCode = 4
        versionName = "1.1-alpha2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["app_name"] = "@string/app_name"
        manifestPlaceholders["app_name_default"] = "MSET9 Installer for Android"
        buildConfigField("boolean", "ENABLE_DEBUG_OPTION", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dbg"
            isDebuggable = true
            manifestPlaceholders["app_name_prefix"] = "D|"
            buildConfigField("boolean", "ENABLE_DEBUG_OPTION", "true")
        }
    }
    flavorDimensions += "artifact"
    productFlavors {
        create("standard") {
            dimension = "artifact"
        }
        create("artifact") {
            dimension = "artifact"
            applicationIdSuffix = ".artifact"
            versionNameSuffix = "-artifact"
            resValue("string", "for_gradle_app_name_prefix", "A|")
            manifestPlaceholders["app_name_prefix"] = "A|"
        }
    }

    applicationVariants.all {
        val variant = this
        defaultConfig.manifestPlaceholders["app_name_default"]?.let { defaultAppName ->
            var appNamePrefix = ""
            variant.productFlavors.forEach { flavor ->
                flavor.manifestPlaceholders["app_name_prefix"]?.let { prefix ->
                    appNamePrefix += prefix
                }
            }
            variant.buildType.manifestPlaceholders["app_name_prefix"]?.let { prefix ->
                appNamePrefix += prefix
            }
            if (appNamePrefix != "") {
                variant.mergedFlavor.manifestPlaceholders["app_name"] = "!$appNamePrefix$defaultAppName"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}