import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = "Multiplatform async helpers — timers, retry, and coroutine-based scheduling."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {

    commonMain {
      dependencies {
        api(libs.kotlinx.coroutines.core)

        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.kotlinCollections))
        api(project(Projects.open.commons.disposable))
        api(project(Projects.open.commons.logging))
      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
      }
    }

    jsTest {
      dependencies {
      }
    }

    jvmMain {
      dependencies {
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
      }
    }
  }
}
