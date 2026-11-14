import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

description = "Annotations"

plugins {
  dependencyFence
}

dependencyFence {
  forbid {
    reason = "annotations is the project's annotation root. It must not pull in any other internal project — annotations need to be importable from anywhere."
    forbidProjectsMatching(Regex("""^:internal:.+$"""))
  }
}


kotlin {
  addAnnotationDependencies(project, Scope.Main)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project, Scope.Test)

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlinx.serialization.core)
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
