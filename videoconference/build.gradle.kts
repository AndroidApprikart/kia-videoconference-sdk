import com.android.build.api.dsl.LintOptions

plugins {
//    id("com.android.application")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.app.vc"
    compileSdk = 33

    defaultConfig {
//        applicationId = "com.app.vc"
        minSdk = 24
        targetSdk = 33
//        versionCode = 1
//        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds =false
    }

}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation(project(mapOf("path" to ":webrtc-android-framework")))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //Hilt - dependency injection /*06 Nov 2023 - Hilt commented and removed*/
//    implementation("com.google.dagger:hilt-android:2.48.1")
//    kapt("com.google.dagger:hilt-android-compiler:2.48.1")

    //Gson
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")
    //for shared vm and vm
    implementation("androidx.activity:activity-ktx:1.7.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.fragment:fragment-ktx:1.5.7")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("androidx.annotation:annotation:1.5.0")
    testImplementation("org.mockito:mockito-core:1.10.19")
    testImplementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.json:json:20210307")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")

    //Logging Interceptor
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")

    //retrofit - API calls
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")


}