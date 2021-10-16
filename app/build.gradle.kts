plugins {
  id("com.android.application")
  id("kotlin-android")
}

android {
  compileSdk = project.compileSdk

  defaultConfig {
    applicationId = "com.ys.coil"
    versionCode = project.versionCode
    versionName = project.versionName
    vectorDrawables.useSupportLibrary = true
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
          getDefaultProguardFile(
              "proguard-android-optimize.txt"
          ),
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
  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {

  implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))

  implementation(Library.ANDROIDX_CORE)
  implementation(Library.ANDROIDX_APPCOMPAT)
  implementation(Library.MATERIAL)
}