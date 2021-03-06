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
		unitTests.isReturnDefaultValues = true
	}
	packagingOptions {
		resources.pickFirsts += "META-INF/AL2.0"
		resources.pickFirsts += "META-INF/LGPL2.1"
	}
}

dependencies {
    implementation(Library.KOTLINX_COROUTINES_ANDROID)
    implementation(Library.KOTLINX_COROUTINES_TEST)

    implementation(Library.ANDROIDX_APPCOMPAT)
    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_LIFECYCLE_COMMON)

    implementation(Library.MATERIAL)

    implementation(Library.OKHTTP)
    implementation(Library.OKHTTP_MOCK_WEB_SERVER)

    implementation(Library.OKIO)

    implementation(Library.ANDROIDX_TEST_CORE)
    implementation(Library.ANDROIDX_TEST_JUNIT)

    implementation(Library.JUNIT)

    testImplementation(Library.JUNIT)
    testImplementation(kotlin("test-junit", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
}
