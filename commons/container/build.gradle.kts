import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """Streamed ZIP container format with a versioned type header, for the JVM and linuxX64"""

plugins {
  kotlinxSerialization
}

kotlin {
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.version))
        api(libs.kotlinx.io.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kmp.zip)
        implementation(libs.kmp.zip.kotlinx)
      }
    }
  }
}
