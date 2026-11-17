import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Time extensions"""


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
        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.kotlinLang))

        api(project(Projects.open.units.unit))
        api(libs.kotlin.reflect)
        api(libs.kotlinx.datetime)

        implementation(libs.kotlinx.serialization.core)
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
        implementation(libs.commons.lang3)
        api(libs.bson.kotlinx)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}
