plugins {
  id("com.android.library")
  id("kotlin-android")
}

android {
  compileSdk = project.compileSdk

  defaultConfig {
    minSdk = project.minSdk
    targetSdk = project.targetSdk
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  libraryVariants.all {
    generateBuildConfigProvider?.configure { enabled = false }
  }
  sourceSets {
    getByName("test").apply {
      assets.srcDirs("src/sharedTest/assets")
      java.srcDirs("src/sharedTest/java")
    }
    getByName("androidTest").apply {
      assets.srcDirs("src/sharedTest/assets")
      java.srcDirs("src/sharedTest/java")
    }
  }
  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
  testOptions {
    unitTests.isIncludeAndroidResources = true
    unitTests.isReturnDefaultValues = true
  }
}

dependencies {
  api(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
  api(Library.KOTLINX_COROUTINES_ANDROID)

  implementation(Library.ANDROIDX_ANNOTATION)
  implementation(Library.ANDROIDX_COLLECTION)
  implementation(Library.ANDROIDX_CORE)
  implementation(Library.ANDROIDX_EXIF_INTERFACE)

  api(Library.ANDROIDX_LIFECYCLE_COMMON)

  // Optional: only use this dependency if it is present in the classpath at runtime.
  compileOnly(Library.ANDROIDX_APPCOMPAT)

  api(Library.OKHTTP)
  api(Library.OKIO)

  addTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
  addAndroidTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
}
