import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addKotlinDependencies

description = """Unit"""

plugins {
  dependencyFence
}

dependencyFence {
  forbid {
    reason = "unit is a base library. Only annotations is allowed; no commons or higher-level modules."
    forbidProjectsMatching(Regex("""^:internal:open:commons:.+$"""))
  }
}

kotlin {
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
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
      }
    }
  }
}
