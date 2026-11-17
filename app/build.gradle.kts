import java.util.Properties
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val mapsApiKey = localProperties.getProperty("MAPS_API_KEY", "")
// Firebase's downloaded Android configuration is ignored by Git. Match this app's
// client rather than accidentally using another Android app in the same project.
val firebaseFile = file("google-services.json")
val firebaseJson = if (firebaseFile.exists()) JsonSlurper().parse(firebaseFile) as Map<*, *> else emptyMap<Any, Any>()
val firebaseProject = firebaseJson["project_info"] as? Map<*, *> ?: emptyMap<Any, Any>()
val firebaseClient = (firebaseJson["client"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.firstOrNull { client ->
    val info = client["client_info"] as? Map<*, *>
    (info?.get("android_client_info") as? Map<*, *>)?.get("package_name") == "com.findit.campus"
}
if (firebaseFile.exists()) require(firebaseClient != null) { "google-services.json must include Android package com.findit.campus." }
val firebaseDefaults = mapOf(
    "FIREBASE_APPLICATION_ID" to (firebaseClient?.get("client_info") as? Map<*, *>)?.get("mobilesdk_app_id"),
    "FIREBASE_API_KEY" to ((firebaseClient?.get("api_key") as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("current_key"),
    "FIREBASE_PROJECT_ID" to firebaseProject["project_id"],
    "FIREBASE_STORAGE_BUCKET" to firebaseProject["storage_bucket"],
    "FIREBASE_MESSAGING_SENDER_ID" to firebaseProject["project_number"],
    "GOOGLE_WEB_CLIENT_ID" to (firebaseClient?.get("oauth_client") as? List<*>)?.filterIsInstance<Map<*, *>>()
        ?.firstOrNull { (it["client_type"] as? Number)?.toInt() == 3 }?.get("client_id"),
)
fun configuredValue(key: String): String = localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
    ?: firebaseDefaults[key]?.toString().orEmpty()
fun String.buildConfigLiteral() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.findit.campus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.findit.campus"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPS_API_KEY", mapsApiKey.buildConfigLiteral())
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        firebaseDefaults.keys.forEach { key ->
            buildConfigField("String", key, configuredValue(key).buildConfigLiteral())
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.google.id)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.window.size)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.compose)

    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.mlkit.text)
    implementation(libs.mlkit.label)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.palette)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(libs.androidx.test.core)
}
