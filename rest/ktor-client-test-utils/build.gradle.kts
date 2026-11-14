import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKtorClientDependencies

description = """Ktor Client Test Utils"""

plugins {
  kotlinxSerialization
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project, Scope.Main)
  addKtorClientDependencies(project, Scope.Main)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.rest.model))

        api(project(Projects.open.rest.ktorClient))

        api(libs.ktor.client.mock)

        api(project(Projects.open.commons.testUtils))
        api(project(Projects.open.commons.kotlinLang))
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
        implementation(libs.kotlinx.serialization.json)

        //Used for Heartbeat demo
        implementation(project(Projects.open.commons.concurrent))
      }
    }
  }
}

