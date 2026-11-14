import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

plugins {
  kotlinxSerialization
}

description = "Kotlin/JS React helper components and hooks — error boundaries, busy state, confirmation dialogs, focus and key handling."

kotlin {
  js {
    browser {
      commonWebpackConfig(Action {
        cssSupport {
          enabled.set(true) //enable CSS support for all tasks (https://kotlinlang.org/docs/js-project-setup.html#building-executables)
        }
      })
    }
    // Disable Node.js tests - this project uses npm packages with CSS (toastr, bootstrap)
    // which cannot be loaded in Node.js environment
    nodejs {
      testTask {
        enabled = false
      }
    }
  }
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {

        api(project(Projects.open.commons.kotlinLang))

        implementation(project(Projects.open.commons.concurrent))
        implementation(project(Projects.open.commons.featureFlags))
        implementation(project(Projects.open.commons.i18n))
        implementation(project(Projects.open.commons.uuid))
        implementation(project(Projects.open.commons.kotlinJs))
        implementation(project(Projects.open.commons.kotlinCollections))

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.html)
      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
        implementation(project(Projects.open.rest.ktorClient)) //isNetworkFailure in StandardExceptionsFormatter

        //BOM for dependencies
        api(project.dependencies.platform(libs.kotlin.wrappers.bom))

        api(libs.kotlin.react)
        api(libs.kotlin.react.dom)
        api(libs.kotlin.styled.next)

        api(npm("toastr", "2.1.4"))

        //Required by kotlin-styled-next for webpack CSS processing
        api(devNpm("style-loader", "4.0.0"))
        api(devNpm("css-loader", "7.1.4"))
      }
    }

    jsTest {
      dependencies {
        implementation(npm("bootstrap", "5.3.8")) //required for BoostrapTests.kt
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
