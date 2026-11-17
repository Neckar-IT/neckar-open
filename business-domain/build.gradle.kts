import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKtorClientDependencies

description = """Shared business primitives: tags, catalog items, edit history, work effort, and value timelines that keep a price or a set of article conditions over time."""

plugins {
  kotlinxSerialization
  generateTypesList
  openapiGenerationConfig
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))

        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.kotlinxSerializers))
        api(project(Projects.open.commons.uuid))
        api(project(Projects.open.commons.domain))
        api(project(Projects.open.financial))
        api(project(Projects.open.user))
        api(project(Projects.open.rest.model))

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
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
        implementation(project(Projects.open.rest.openapiGenerator))

        addKtorClientDependencies(project, Scope.Test)
      }
    }
  }
}
