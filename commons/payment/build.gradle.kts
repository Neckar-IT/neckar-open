import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """Payment value-types (CardNumber, CardNetwork, ExpiryDate, MandateReference, ...)"""

plugins {
  kotlinxSerialization
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

        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.time))
        api(libs.kotlinx.datetime)
      }
    }

    jvmTest {
      dependencies {
      }
    }
    commonTest {
      dependencies {
      }
    }
  }
}
