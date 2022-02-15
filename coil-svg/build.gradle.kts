import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
	id("com.android.library")
	id("kotlin-android")
}

// android {
// 	compileSdk = project.compileSdk
//
// 	defaultConfig {
// 		minSdk = project.minSdk
// 		targetSdk = project.targetSdk
// 		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
// 	}
// 	buildTypes {
// 		release {
// 			isMinifyEnabled = false
// 			proguardFiles(
// 				getDefaultProguardFile(
// 					"proguard-android-optimize.txt"
// 				),
// 				"proguard-rules.pro"
// 			)
// 		}
// 	}
// 	compileOptions {
// 		sourceCompatibility = JavaVersion.VERSION_1_8
// 		targetCompatibility = JavaVersion.VERSION_1_8
// 	}
// 	kotlinOptions {
// 		jvmTarget = "1.8"
// 	}
// 	libraryVariants.all {
// 		generateBuildConfigProvider?.configure { enabled = false }
// 	}
// 	testOptions {
// 		unitTests.isIncludeAndroidResources = true
// 	}
// }

setupLibraryModule()

dependencies {
	api(project(":coil-base"))

	implementation(Library.ANDROIDX_CORE)
	implementation(Library.ANDROID_SVG)

	addTestDependencies(KotlinCompilerVersion.VERSION)
	addAndroidTestDependencies(KotlinCompilerVersion.VERSION)
}
