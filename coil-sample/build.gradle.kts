plugins {
  id("com.android.application")
  id("kotlin-android")
}

setupAppModule {
  defaultConfig {
    applicationId = "com.ys.coil.sample"
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles("shrinker-rules.pro", "shrinker-rules-android.pro")
      signingConfig = signingConfigs["debug"]
    }
  }
  buildFeatures {
    viewBinding = true
  }
}

dependencies {

  implementation(project(":coil-default"))

  implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))

  implementation(Library.ANDROIDX_APPCOMPAT)
  implementation(Library.ANDROIDX_CONSTRAINT_LAYOUT)
  implementation(Library.ANDROIDX_CORE)
  implementation(Library.ANDROIDX_LIFECYCLE_EXTENSIONS)
  implementation(Library.ANDROIDX_LIFECYCLE_LIVE_DATA)
  implementation(Library.ANDROIDX_LIFECYCLE_VIEW_MODEL)
  implementation(Library.ANDROIDX_MULTIDEX)
  implementation(Library.ANDROIDX_RECYCLER_VIEW)

  implementation(Library.MATERIAL)
}
