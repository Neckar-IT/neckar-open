import it.neckar.projects.project

import it.neckar.gradle.ksp.generating.ts.generateTypeScriptDefinitions

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = "REST paging, document version, sort and archive filter types, free of domain dependencies."

plugins {
  kotlinxSerialization
  kspSerialization
  generateTypesList
  generateTsDeclaration
  dependencyFence
}

dependencyFence {
  forbid {
    reason = "rest:model-paging is reached by persistence modules. Only annotations, commons and units are allowed; no domain modules."
    forbidProjectsMatching(Regex("""^:internal:(?!open:(annotations|commons:.+|units:.+)$).+$"""))
  }
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.lifeCycle))

        api(libs.kotlinx.serialization.core)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}

generateTypeScriptDefinitions {
}
