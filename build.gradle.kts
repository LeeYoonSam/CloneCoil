buildscript {
  apply(from = "buildSrc/plugins.gradle.kts")
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies {
    classpath(rootProject.extra["androidPlugin"].toString())
    classpath(rootProject.extra["kotlinPlugin"].toString())
    // classpath(rootProject.extra["binaryCompatibilityPlugin"].toString())
    // classpath(rootProject.extra["ktlintPlugin"].toString())
  }
}

// apply(plugin = "binary-compatibility-validator")
//
// extensions.configure<kotlinx.validation.ApiValidationExtension> {
//   ignoredProjects = mutableSetOf("coil-sample", "coil-test")
// }

allprojects {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
    }
  }

  tasks.withType<Test> {
    testLogging {
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
      events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED, org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED, org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
      showStandardStreams = true
    }
  }
}
