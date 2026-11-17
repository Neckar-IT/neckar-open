import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Kotlinx Serialization serializers"""

plugins {
  kotlinxSerialization
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))

        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.kotlinCollections)) //required to compile the serializers
        api(project(Projects.open.commons.kotlinBytearray)) //required to compile the serializers
        api(project(Projects.open.commons.i18n)) //required for DoubleAsIsoDateTimeSerializer

        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.json)
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
        api(project(Projects.open.commons.concurrent))
        api(libs.kotlin.reflect) //for plausibility checks
        api(libs.bson.kotlinx)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))

        implementation(project(Projects.open.commons.kotlinLang))

        implementation(project(Projects.open.commons.kotlinCollections))
        implementation(project(Projects.open.commons.kotlinBytearray))
        implementation(project(Projects.open.commons.uuid))
        implementation(project(Projects.open.commons.version)) //to compare the bson-kotlinx version numerically
      }
    }
  }
}
