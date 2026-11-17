import it.neckar.projects.project

import it.neckar.gradle.ksp.generating.ts.generateTypeScriptDefinitions

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = """Kotlin Lang - Multiplatform"""

plugins {
  kotlinxSerialization
  generateTypesList
  generateTsDeclaration
  dependencyFence
}

dependencyFence {
  forbid {
    reason = "kotlin-lang sits near the root of the open-commons dependency tree. Production deps are " +
      "limited to annotations, units/unit and commons/descriptors — any other open-commons " +
      "project dep would risk cycles and break the layering. descriptors is allowed because it has no " +
      "internal-project deps of its own and owns the canonical Port/runtime-context types that kotlin-lang " +
      "consumes."

    forbidProjectsMatching(Regex("""^:internal:open:commons:(?!(kotlin-lang|descriptors)$).+$"""))
  }
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.units.unit))
        api(project(Projects.open.annotations))
        api(project(Projects.open.commons.descriptors))
        api(libs.kotlin.reflect)

        // api: public types such as Url are @Serializable; their companions extend serialization types.
        api(libs.kotlinx.serialization.core)
      }
    }

    commonTest {
      dependencies {
      }
    }

    webMain {
      dependencies {
        implementation(libs.kotlinx.browser)
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

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}

generateTypeScriptDefinitions {
}
