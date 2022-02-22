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
	libraryVariants.all {
		generateBuildConfigProvider?.configure { enabled = false }
	}
	testOptions {
		unitTests.isIncludeAndroidResources = true
	}
	buildFeatures {
		compose = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = Library.COMPOSE_VERSION
	}
}

dependencies {
	api(project(":coil-base"))

	implementation(Library.ANDROIDX_CORE)

	implementation(Library.ACCOMPANIST_DRAWABLE_PAINTER)

	api(Library.COMPOSE_UI)

	addTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
	addAndroidTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)

	androidTestImplementation(Library.COMPOSE_FOUNDATION)
	androidTestImplementation(Library.COMPOSE_UI_TEST_JUNIT)
	androidTestImplementation(Library.COMPOSE_UI_TEST_MANIFEST)
}

