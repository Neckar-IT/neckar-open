import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "Kotlin/JS bindings and DSL for Redux state management with React."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {

        implementation(project(Projects.open.commons.logging))
      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
        implementation(project(Projects.open.commons.kotlinReact))

        //BOM for dependencies
        implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))

        implementation(libs.kotlin.react)
        implementation(libs.kotlin.react.dom)
        implementation(libs.kotlin.styled.next)

        api(npm("react-redux", "9.2.0"))
        api(npm("redux", "5.0.1"))
        implementation(libs.kotlinx.serialization.json)
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
      }
    }
  }
}
