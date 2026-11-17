import it.neckar.projects.project

import it.neckar.gradle.ksp.generating.ts.generateTypeScriptDefinitions
import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Geometry"""

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

        implementation(libs.kotlinx.serialization.core)

        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.units.unit))
        api(project(Projects.open.annotations))
        api(project(Projects.open.commons.i18n))

      }
    }

    commonTest {
      dependencies {
      }
    }

    jsMain {
      dependencies {
        api(project(Projects.open.commons.kotlinJs))
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

generateTypeScriptDefinitions {
}
