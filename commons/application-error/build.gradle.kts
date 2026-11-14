import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "Application error model — expected vs. unexpected errors as typed, serializable problems for REST and UI."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {

        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.exceptions))
        api(project(Projects.open.commons.kotlinxSerializers))
        api(project(Projects.open.commons.tracing))

        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
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
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}
