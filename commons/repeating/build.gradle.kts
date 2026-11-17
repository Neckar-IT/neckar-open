import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = "Recurring background jobs on coroutines: one block, one rhythm, and the properties a hand-rolled while-delay loop would have to reinvent every time."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.units.unit))

        api(libs.kotlinx.coroutines.core)

        api(project(Projects.open.commons.disposable))
        api(project(Projects.open.commons.logging))

        //For the platform locks (PlatformReadWriteLock) and the mockable now()
        api(project(Projects.open.commons.kotlinLang))
      }
    }

    commonTest {
      dependencies {
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
      }
    }
  }
}
