import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
plugins {
}

description = """Kotlinx ByteArray - Multiplatform"""

kotlin {
  addKotlinDependencies(project)
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinCollections))
        api(project(Projects.open.commons.kotlinLang))
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

detekt {
  //Silences the plain `detekt` task for the auto-imported KDS code in this module.
  //Scope note: this affects ONLY that task - the per-source-set and per-compilation tasks
  //(detektCommonMainSourceSet, detektMainJvm, ...) set their own source and keep analysing
  //the module, which is what the committed baselines here are for.
  source.setFrom()
}
