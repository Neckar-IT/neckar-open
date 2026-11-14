import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "Tracing primitives — the OpenTelemetry trace ID as a value class, shared by error responses, health projections and the response header."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
        implementation(libs.kotlinx.serialization.core)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}
