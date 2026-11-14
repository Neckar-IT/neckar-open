import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Exceptions stuff"""


plugins {
  kotlinxSerialization
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {

    commonMain {
      dependencies {
        api(project(Projects.open.annotations))

        api(project(Projects.open.commons.uuid))

        api(libs.kotlinx.serialization.core)
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
