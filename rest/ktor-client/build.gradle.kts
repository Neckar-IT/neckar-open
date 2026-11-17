import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKtorClientDependencies

description = """KTOR HTTP client - multiplatform"""

plugins {
  kotlinxSerialization
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)
  addKtorClientDependencies(project, Scope.Main)

  sourceSets {
    commonMain {
      dependencies {
        api(libs.ktor.client.auth)
        //only with Ktor 3
        //api("io.ktor:ktor-client-call-id:_")

        api(project(Projects.open.rest.model))
        api(project(Projects.open.commons.featureFlags))
        api(project(Projects.open.commons.kotlinJs))
        api(project(Projects.open.commons.applicationError))

        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.logging))
        api(project(Projects.open.commons.heartbeat))
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
        implementation(project(Projects.open.versionInfo))
        implementation(libs.kotlinx.serialization.json)

        //Used for Heartbeat demo
        implementation(project(Projects.open.commons.concurrent))
        implementation(libs.ktor.server.test.host)

        implementation(libs.ktor.client.apache)
        implementation(libs.ktor.client.cio)
        implementation(libs.ktor.client.jetty)
        implementation(libs.ktor.client.okhttp)
      }
    }
  }
}

