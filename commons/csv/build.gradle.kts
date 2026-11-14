import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinMultiPlatform
  kotlinxSerialization
}

description = "Multiplatform CSV loading and conversion utilities."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.resourceLoading))
      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
        //Only the js/jvm actuals use kotlin-csv - the library publishes no wasmJs variant
        implementation(libs.kotlin.csv)
      }
    }

    jsTest {
      dependencies {
      }
    }

    jvmMain {
      dependencies {
        //Only the js/jvm actuals use kotlin-csv - the library publishes no wasmJs variant
        implementation(libs.kotlin.csv)
      }
    }

    jvmTest {
      dependencies {
      }
    }
  }
}
