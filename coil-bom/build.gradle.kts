plugins {
	id("com.android.library")
	id("kotlin-android")
}

setupLibraryModule {
	compileSdk = project.compileSdk
}

dependencies {
	constraints {
		api(project(":coil-singleton"))
		api(project(":coil-base"))
		api(project(":coil-gif"))
	}
}
