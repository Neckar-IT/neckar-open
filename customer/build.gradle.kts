import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Customer and company master data: postal address with a draft/confirmed pair, contact and bank details, IBAN, BIC, tax and registration numbers."""

plugins {
  kotlinxSerialization
  generateTsDeclaration
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

        implementation(libs.kotlinx.serialization.core)

        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.i18n))
        api(project(Projects.open.commons.kotlinxSerializers))
        api(project(Projects.open.commons.uuid))
        api(project(Projects.open.commons.lifeCycle))
        api(project(Projects.open.contact))
        api(project(Projects.open.rest.model))
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
      }
    }
  }
}
