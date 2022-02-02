plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule()

dependencies {
    api(project(":coil-base"))

    implementation(Library.ANDROIDX_CORE)
    implementation(Library.ANDROIDX_VECTOR_DRAWABLE_ANIMATED)

    addTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
    addAndroidTestDependencies(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
}
