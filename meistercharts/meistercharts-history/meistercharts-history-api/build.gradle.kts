import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

plugins {
  kotlinxSerialization
  kspSerialization
}

description = "Meistercharts - History API — REST query model and storage access for time-series history data."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.meistercharts.meisterchartsHistory.meisterchartsHistoryCore))
        api(project(Projects.open.units.unit))

        api(libs.kotlinx.serialization.json)
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
        api(libs.threeten.extra)
        api(libs.kotlinx.coroutines.core)
        implementation(project(Projects.open.commons.io))
        implementation(libs.commons.compress)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))

        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}
