import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "Kotlin/JS React Router bindings — hooks, breadcrumbs, tabs, and typed navigation."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {

        implementation(project(Projects.open.commons.uuid))
        implementation(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.kotlinEvents))
        implementation(project(Projects.open.commons.kotlinCollections))

        implementation(project(Projects.open.commons.kotlinReact))
      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
        //BOM for dependencies
        implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))

        implementation(libs.kotlin.react)
        implementation(libs.kotlin.react.dom)
        implementation(libs.kotlin.styled.next)
        implementation(libs.kotlin.react.router.dom)
        implementation(project(Projects.open.commons.logging))
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
