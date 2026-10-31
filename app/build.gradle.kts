import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

val configuredApiBaseUrl = (
    providers.gradleProperty("THEVAULT_API_BASE_URL").orNull
        ?: providers.environmentVariable("THEVAULT_API_BASE_URL").orNull
        ?: localProperties.getProperty("THEVAULT_API_BASE_URL")
    )?.trim()?.trimEnd('/')

val debugApiBaseUrl = configuredApiBaseUrl
    ?.takeIf { it.isNotBlank() }
    ?: "http://10.0.2.2:8000"
val releaseApiBaseUrl = configuredApiBaseUrl
    ?.takeIf { it.isNotBlank() }
    ?: "https://api.invalid"

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""


val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.thevault"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.thevault"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", debugApiBaseUrl.asBuildConfigString())
        }
        release {
            buildConfigField("String", "API_BASE_URL", releaseApiBaseUrl.asBuildConfigString())
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    ksp(libs.androidx.room.compiler)
}

val validateReleaseApiUrl = tasks.register("validateReleaseApiUrl") {
    group = "verification"
    description = "Verifies that release builds use a valid HTTPS API endpoint."
    inputs.property("apiUrl", configuredApiBaseUrl.orEmpty())

    doLast {
        val apiUrl = inputs.properties.getValue("apiUrl") as String
        require(apiUrl.isNotBlank()) {
            "Define THEVAULT_API_BASE_URL with the public HTTPS URL of the AWS API."
        }
        val uri = runCatching { URI(apiUrl) }.getOrElse {
            throw GradleException("THEVAULT_API_BASE_URL is not a valid URL: $apiUrl", it)
        }
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
            "Release API URL must use HTTPS and include a host: $apiUrl"
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseApiUrl)
}
