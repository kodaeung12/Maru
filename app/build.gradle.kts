import java.util.Properties

plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localFile = rootProject.file("local.properties")

if (localFile.exists()) {
    localFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

val seoulApiKey = localProperties.getProperty("SEOUL_API_KEY", "")
val kakaoMapApiKey = localProperties.getProperty("KAKAO_MAP_API_KEY", "").trim()

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))


    implementation("com.google.firebase:firebase-analytics")


    implementation("com.google.firebase:firebase-auth")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.0")
}

android {
    buildFeatures {
        viewBinding = true

        buildConfig = true
    }
    namespace = "com.project.maru"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.project.maru"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SEOUL_API_KEY", "\"$seoulApiKey\"")

        buildConfigField("String", "KAKAO_MAP_API_KEY", "\"$kakaoMapApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


    }

    buildTypes {
        release {
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.viewpager2)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.googleid)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("com.kakao.maps.open:android:2.15.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

}
