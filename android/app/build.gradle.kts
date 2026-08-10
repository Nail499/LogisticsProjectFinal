plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// google-services plugini YALNIZ google-services.json faylı mövcuddursa
// tətbiq olunur — plugin özü bu fayl yoxdursa build-i sındırır ("File
// google-services.json is missing"), ona görə istifadəçi öz Firebase
// layihəsini qoşana qədər (bax android/README.md) tətbiq normal build
// olunmalıdır. Plugin `apply false` ilə artıq kök build.gradle.kts-də
// classpath-ə əlavə olunub, burada yalnız şərti tətbiq edilir.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "az.fleetra.mobile"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "az.fleetra.mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.play.services.location)
    // google-services.json olmasa belə bunlar rahat compile olunur — Firebase
    // SDK-nın öz FirebaseInitProvider-i FirebaseApp-i tapa bilmirsə sadəcə
    // xəbərdarlıq loglayır, tətbiqi çökdürmür (bax messaging/FleetraFcmService.kt
    // şərhi). Push YALNIZ google-services plugini tətbiq olunanda (yəni real
    // google-services.json əlavə edildikdə) faktiki işləyəcək.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}