plugins {
    id("com.google.gms.google-services")
    id("com.android.application")
}

android {
    namespace = "com.example.smartport"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smartport"
        minSdk = 24
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
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

    packagingOptions {
        pickFirst("**/libc++_shared.so")
        pickFirst("META-INF/DEPENDENCIES")
        pickFirst("META-INF/LICENSE")
        pickFirst("META-INF/LICENSE.txt")
        pickFirst("META-INF/license.txt")
        pickFirst("META-INF/NOTICE")
        pickFirst("META-INF/NOTICE.txt")
        pickFirst("META-INF/notice.txt")
        pickFirst("META-INF/ASL2.0")
    }
}

dependencies {
    // Firebase BOM 统一管理版本
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore:24.9.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")

    // Firebase UI Auth
    implementation("com.firebaseui:firebase-ui-auth:9.1.1")

    // Facebook SDK（注释掉，未来再加）
    implementation("com.facebook.android:facebook-login:latest.release")
    implementation("com.facebook.android:facebook-core:latest.release")
    implementation("com.facebook.android:facebook-share:latest.release")
    implementation("com.facebook.android:facebook-common:latest.release")

    // Volley
    implementation("com.android.volley:volley:1.2.1")

    // RecyclerView 和 CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ExifInterface
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    //mqtt
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.1")

    //googlemap
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // AndroidX 核心库
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}