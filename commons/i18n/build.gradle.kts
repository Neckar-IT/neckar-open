import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """MPP I18n"""

plugins {
  kotlinxSerialization
  generateTypesList
  generateTsDeclaration
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
        api(project(Projects.open.commons.datetimeMinimal))
        api(project(Projects.open.commons.time))

        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.datetime)

        implementation(project(Projects.open.commons.kotlinCollections))
        implementation(project(Projects.open.commons.logging))
        implementation(project(Projects.open.commons.kotlinLang))
      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
        api(npm("i18next", "25.8.0"))
        api(npm("i18next-http-backend", "2.5.2"))
      }
    }

    jsTest {
      dependencies {
      }
    }

    jvmMain {
      dependencies {
        api(project(Projects.open.commons.time))
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}
