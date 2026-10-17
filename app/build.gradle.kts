import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val kelkinSigningProperties = Properties().apply {
    val signingFile = rootProject.file("keystore.properties")
    if (signingFile.isFile) signingFile.inputStream().use { load(it) }
}
fun signingValue(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull?.takeIf { it.isNotBlank() }
        ?: kelkinSigningProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.example.kelkin"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }


    defaultConfig {
        applicationId = "com.example.kelkin"
        minSdk = 23
        targetSdk = 36
        versionCode = 21
        versionName = "1.93"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    flavorDimensions += "device"
    productFlavors {
        create("tv") {
            dimension = "device"
            buildConfigField("boolean", "IS_TV", "true")
        }
        create("mobile") {
            dimension = "device"
            buildConfigField("boolean", "IS_TV", "false")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }


    signingConfigs {
        create("release") {
            storeFile = rootProject.file(signingValue("KELKIN_KEYSTORE_PATH", "storeFile") ?: "app/kelkin.jks")
            storePassword = signingValue("KELKIN_KEYSTORE_PASSWORD", "storePassword")
            keyAlias = signingValue("KELKIN_KEY_ALIAS", "keyAlias") ?: "key0"
            keyPassword = signingValue("KELKIN_KEY_PASSWORD", "keyPassword")
        }
    }


    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.11.0")


    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-auth")


    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.0")
    implementation("androidx.media3:media3-session:1.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")


    "tvImplementation"("androidx.leanback:leanback:1.0.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.android.gms:play-services-auth:21.0.0")



    implementation("androidx.navigation:navigation-compose:2.8.0")






}
