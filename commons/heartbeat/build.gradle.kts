import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = """Heartbeat stuff"""

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {

    commonMain {
      dependencies {
        api(project(Projects.open.units.unit))
        api(libs.kotlinx.coroutines.core)

        api(project(Projects.open.commons.logging))
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
        api(project(Projects.open.commons.concurrent))
        api(project(Projects.open.commons.kotlinLang))
      }
    }

    jvmTest {
      dependencies {
      }
    }
  }
}
