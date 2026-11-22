import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project
import it.neckar.projects.sharing.OpenSharedFiles
import it.neckar.projects.sharing.publishFiles

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
        implementation(libs.kotlinx.serialization.json)
      }
    }
    commonTest {
      dependencies {
      }
    }
  }
}

publishFiles(
  OpenSharedFiles.PaymentFormatCases,
  file("src/jvmTest/resources/CardNumber.cases.json"),
  file("src/jvmTest/resources/MandateReference.cases.json"),
  file("src/jvmTest/resources/ExpiryDate.cases.json"),
  file("src/jvmTest/resources/CardNetwork.cases.json"),
)
