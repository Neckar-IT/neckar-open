import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "JUnit and AssertK test helpers and extensions — system-output capture, temporary files, headless guards, and awaitility support."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project, Scope.Main) //this is correct!

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.kotlinLang))

        api(project(Projects.open.commons.crypt))
        api(project(Projects.open.commons.logging))
      }
    }

    commonTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.testData))
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
        api(libs.guava)
        api(libs.commons.io)
        api(libs.commons.lang3)

        api(libs.jackson.databind)
        api(libs.jackson.datatype.jdk8)
        api(libs.jackson.datatype.jsr310)
        api(libs.jackson.datatype.guava)
        api(libs.jackson.dataformat.yaml)
        api(libs.jackson.module.kotlin)
        api(libs.jackson.module.parameter.names)

        api(libs.jsonassert)

        implementation(libs.slf4j.api)

        //Testcontainers
        //GenericContainer is publishPortsOnDockerHostOnly's receiver, but every consumer that calls it
        //declares Testcontainers itself — so implementation, not api.
        implementation(libs.testcontainers)
        implementation(libs.docker.java.api) //CreateContainerCmd, Ports — only on internal declarations
      }
    }

    jvmTest {
      dependencies {
        implementation(libs.docker.java.api) //CreateContainerCmd, HostConfig, Ports in PublishPortsOnDockerHostOnlyTest
      }
    }
  }
}
