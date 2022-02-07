plugins {
	id("com.android.library")
	id("kotlin-android")
}

setupLibraryModule()

dependencies {
	api(project(":coil-base"))

	addTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
	addAndroidTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
}
