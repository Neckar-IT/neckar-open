import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.JsTargetType
import it.neckar.gradle.WebpackModuleType
import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.executableJsApplication

plugins {
  kotlinxSerialization
}

kotlin {
  js {
    executableJsApplication(
      varName = "kotlinReactDemo",
      webpackModuleType = WebpackModuleType.ModernModule,
      webpackModuleTypeForDev = WebpackModuleType.ModernModule,
    )
  }

  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {

        implementation(project(Projects.open.commons.kotlinJs))
        implementation(project(Projects.open.commons.kotlinReact))
        implementation(project(Projects.open.commons.featureFlags))

        implementation(libs.ktor.client.serialization)
        implementation(libs.ktor.serialization.kotlinx.json)

        implementation(libs.ktor.client.serialization)

        implementation(libs.kotlinx.coroutines.core)
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

        implementation(libs.kotlin.css)

        //implementation(npm("toastr", "_"))
        //implementation(npm("formik", "^2.2.9"))

        //Currently unused
        //implementation(npm("bootstrap", "_"))
        //implementation(npm("@popperjs/core", "_"))
        //
        //implementation(npm("@material-ui/core", "_"))
        //implementation(npm("@mui/icons-material", "_"))
        //
        //implementation(npm("react-json-view", "_"))
        implementation(project(Projects.open.commons.logging))
        implementation(project(Projects.open.commons.uuid))
        implementation(project(Projects.open.versionInfo))
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
