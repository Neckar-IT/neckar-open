import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = "REST health and readiness wire types, free of domain dependencies."

plugins {
  kotlinxSerialization
  kspSerialization
  generateTypesList
  dependencyFence
}

dependencyFence {
  forbid {
    reason = "rest:model-health is reached by commons:health. Only annotations, commons and units are allowed; no domain modules."
    forbidProjectsMatching(Regex("""^:internal:(?!open:(annotations|commons:.+|units:.+)$).+$"""))
  }
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinxSerializers))
        api(project(Projects.open.commons.tracing))

        api(libs.kotlinx.serialization.core)
      }
    }
  }
}
