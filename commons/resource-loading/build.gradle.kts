import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.configureWasmJsTarget

plugins {
  kotlinMultiPlatform
  kotlinxSerialization
}

description = "Multiplatform resource loading from classpath, filesystem, or HTTP fetch."

kotlin {
  configureWasmJsTarget(project, wasmJsMainDependencies = {
    implementation(libs.kotlinx.browser)
  })
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.logging))

        implementation(libs.kotlinx.coroutines.core)
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
      }
    }
  }
}
