import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Quote arithmetic: priced elements and items with optionality, VAT handling and flat projections, plus financing and yearly-cost calculations."""

plugins {
  kotlinxSerialization
  generateTsDeclaration
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))
        api(project(Projects.open.commons.money))

        implementation(libs.kotlinx.serialization.core)
        implementation(project(Projects.open.commons.kotlinCollections))

        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.i18n))
        api(project(Projects.open.commons.kotlinxSerializers))
        api(project(Projects.open.commons.uuid))
        api(project(Projects.open.commons.tags))
        api(project(Projects.open.customer))

        api(libs.kotlinx.datetime)
        api(libs.kotlinx.html)
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
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}
