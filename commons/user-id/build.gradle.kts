import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
  kspSerialization
  generateTypesList
}

description = "User ID — the value class naming a user, free of domain dependencies."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))
        api(project(Projects.open.commons.uuid))

        implementation(libs.kotlinx.serialization.core)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}
