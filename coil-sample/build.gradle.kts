plugins {
	id("com.android.application")
	id("kotlin-android")
}

android {
	compileSdk = project.compileSdk

	defaultConfig {
		applicationId = "com.ys.coil.sample"
		versionCode = project.versionCode
		versionName = project.versionName
		vectorDrawables.useSupportLibrary = true
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		minSdkVersion(24)
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
	buildFeatures {
		viewBinding = true
	}
}

dependencies {
	implementation(project(":coil-singleton"))
	implementation(project(":coil-gif"))
	implementation(project(":coil-svg"))

	implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
	implementation(Library.ANDROIDX_ACTIVITY)
	implementation(Library.ANDROIDX_APPCOMPAT)
	implementation(Library.ANDROIDX_CONSTRAINT_LAYOUT)
	implementation(Library.ANDROIDX_CORE)
	implementation(Library.ANDROIDX_LIFECYCLE_VIEW_MODEL)
	implementation(Library.ANDROIDX_RECYCLER_VIEW)
	implementation(Library.MATERIAL)
}
