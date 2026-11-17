import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Shared health layer (service state, dependency probes, uncaught-exception collector)"""

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.concurrent))
        api(project(Projects.open.commons.repeating))
        api(project(Projects.open.commons.disposable))
        api(project(Projects.open.commons.exceptions))
        api(project(Projects.open.commons.logging))
        api(project(Projects.open.commons.tracing))
        api(project(Projects.open.rest.modelHealth))

        api(libs.kotlinx.coroutines.core)
      }
    }

    commonTest {
      dependencies {
      }
    }

    jvmMain {
      dependencies {
        // OTel API only — kept as implementation so it does not leak onto consumers' compile
        // classpath (the public recorder function exposes no OTel type).
        implementation(libs.opentelemetry.api)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.opentelemetry.sdk.testing)
      }
    }
  }
}
