import org.jlleitschuh.gradle.ktlint.KtlintExtension

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
		classpath(rootProject.extra["ktlintPlugin"].toString())
		classpath(rootProject.extra["binaryCompatibilityPlugin"].toString())
	}
}

apply(plugin = "binary-compatibility-validator")

extensions.configure<kotlinx.validation.ApiValidationExtension> {
  ignoredProjects = mutableSetOf("coil-sample", "coil-test")
}

allprojects {

	repositories {
		google()
		mavenCentral()
	}

	group = project.groupId
	version = project.versionName

	apply(plugin = "org.jlleitschuh.gradle.ktlint")

	extensions.configure<KtlintExtension>("ktlint") {
		version by "0.42.1"
		disabledRules by setOf("indent", "max-line-length", "parameter-list-wrapping")
	}
}
