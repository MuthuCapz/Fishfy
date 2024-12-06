plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.capztone.fishfy"
    compileSdk = 34



    flavorDimensions("env")
    productFlavors {
        create("dev") {
            applicationId = "com.capztone.fishfy"
            minSdk = 24
            targetSdk = 34
            versionCode = 20
            versionName = "1.3.5"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            dimension = "env"
            buildConfigField(
                "String",
                "FIREBASE_DB_URL",
                "\"https://sea1-2a585-default-rtdb.firebaseio.com/\""
            )
        }
        create("prod") {
            applicationId = "com.capztone.fishfy"
            minSdk = 24
            targetSdk = 34
            versionCode = 20
            versionName = "1.3.5"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            dimension = "env"
            buildConfigField(
                "String",
                "FIREBASE_DB_URL",
                "\"https://fishfyproduction.firebaseio.com/\""
            )
        }
    }


buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
        dataBinding = true
        buildConfig = true
    }



}
dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")
    implementation("com.airbnb.android:lottie:4.2.0")

//viewpager2
    implementation ("com.hbb20:ccp:2.6.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation("com.google.firebase:firebase-crashlytics-buildtools:3.0.2")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")
//indicator
    implementation ("me.relex:circleindicator:2.1.6")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")// or any compatible version
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")

    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.github.Marvel999:Android-Loading-Animation:1.0.0")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    implementation("com.google.firebase:firebase-auth:23.1.0")


    testImplementation("junit:junit:4.13.2")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.razorpay:checkout:1.6.40")

}


