import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """User model"""

plugins {
  kotlinxSerialization
  kspSerialization
  generateTypesList
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

        api(project(Projects.open.commons.authorization))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.kotlinxSerializers))
        api(project(Projects.open.commons.multiTenancy))
        api(project(Projects.open.commons.userId))
        api(project(Projects.open.commons.uuid))
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
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}
