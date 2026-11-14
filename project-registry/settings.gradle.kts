pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  // Same plugin and version as the build-logic/settings.gradle.kts of the monorepo.
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// An included build reads no gradle.properties of the build including it: the export of internal/open writes the build
// platform release into internal/open/gradle.properties, inside the monorepo that file names none.
val buildplatformVersion: String? = providers.fileContents(layout.rootDirectory.file("../gradle.properties")).asText.orNull
  ?.lineSequence()
  ?.map { it.trim() }
  ?.firstOrNull { it.startsWith("buildplatformVersion=") }
  ?.substringAfter('=')

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    // The Spotless plugin of the settings plugin it.neckar.open.settings.
    gradlePluginPortal()
  }
  versionCatalogs {
    create("libs") {
      if (buildplatformVersion == null) {
        from(files("../../buildplatform/gradle/libs.versions.toml"))
      } else {
        from("it.neckar.buildplatform:version-catalog:$buildplatformVersion")
      }
    }
  }
}

rootProject.name = "open-project-registry"

if (buildplatformVersion == null) {
  // The registry types that `open-projects` instantiates, from the sources of the monorepo.
  includeBuild("../../buildplatform")
} else {
  // The build scripts name the artifacts of the build platform without a version, as inside the monorepo. A constraint on
  // `api` gives them the version of the release the export names, also in the build that puts `registry` on its classpath.
  gradle.beforeProject {
    plugins.withId("java-library") {
      listOf("conventions", "projects").forEach { artifact ->
        dependencies.constraints.add("api", "it.neckar.buildplatform:$artifact:$buildplatformVersion")
      }
    }
  }
}

// The registry of the MeisterCharts projects, which the export of internal/open/meistercharts reads without this build.
includeBuild("../meistercharts/project-registry") { name = "meistercharts-project-registry" }

include("open-projects")
include("registry")
