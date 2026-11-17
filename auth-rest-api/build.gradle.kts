import it.neckar.projects.project

import it.neckar.gradle.ksp.generating.ts.generateTypeScriptDefinitions

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """The wire format of an authentication service: login by credentials or token, registration, password reset, impersonation and JWT refresh."""

plugins {
  kotlinxSerialization
  kspSerialization
  generateTsDeclaration
  openapiGenerationConfig
  specHarvest
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))

        api(project(Projects.open.rest.model))

        implementation(libs.kotlinx.serialization.core)

        api(project(Projects.open.user))
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

        implementation(libs.ktor.client.okhttp)
        implementation(project(Projects.open.rest.openapiGenerator))
      }
    }
  }
}

generateTypeScriptDefinitions {
}
