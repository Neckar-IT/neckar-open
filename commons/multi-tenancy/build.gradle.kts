import it.neckar.projects.project

import it.neckar.projects.Projects
import it.neckar.projects.sharing.OpenSharedFiles
import it.neckar.projects.sharing.publishFiles

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "Multi-tenancy primitives — tenant descriptors, names, slugs, and tenant-scoped ownership."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.kotlinCollections))
        api(project(Projects.open.commons.uuid))
        api(project(Projects.open.commons.kotlinxSerializers))

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
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}

publishFiles(OpenSharedFiles.SlugCases, file("src/jvmTest/resources/Slug.cases.json"))
