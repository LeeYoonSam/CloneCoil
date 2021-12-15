dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "CloneCoil"
include(":coil-sample")
include(":coil-base")
include(":coil-default")
include(":coil-gif")
