import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = """App stuff"""

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        // clikt-core, not clikt: the Mordant-backed artifact repackages the core symbols and
        // breaks the Kotlin/Native link for consumers bringing their own Mordant.
        api(libs.clikt.core)

        // The file system behind the `.env` lookup, on every target. `api`, because Path is part
        // of the FileSystemAccess signature every consumer calls.
        api(libs.kotlinx.io.core)
      }
    }

    jvmMain {
      dependencies {
        api(project(Projects.open.annotations))

        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.concurrent))
        api(project(Projects.open.commons.crypt))
        api(project(Projects.open.commons.version))
        api(project(Projects.open.commons.logging))
        api(project(Projects.open.commons.disposable))
        api(project(Projects.open.commons.health))

        api(libs.clikt)

        api(libs.guava)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(libs.commons.codec)
      }
    }
  }
}
