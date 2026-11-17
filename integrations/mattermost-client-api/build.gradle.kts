import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "The Mattermost REST API client and its wire types."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
        // api: the MattermostClient constructor takes a ktor HttpClientEngine.
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
        // api: findUserIdByEmail takes an EmailAddress.
        api(project(Projects.open.contact))

        implementation(project(Projects.open.commons.logging))
        implementation(libs.ktor.client.cio)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(libs.ktor.client.mock)

        // Required by the manual MattermostDemo (live API client)
        implementation(libs.ktor.client.okhttp)
        implementation(project(Projects.open.commons.i18n))
      }
    }
  }
}
