import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """Meistercharts - Core"""

plugins {
  kotlinxSerialization
  kspBoxing
}

kotlin {
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))

        api(project(Projects.open.commons.i18n))
        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.geometry))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.kotlinAsync))
        api(project(Projects.open.commons.disposable))
        api(project(Projects.open.commons.observable))
        api(project(Projects.open.commons.provider))
        api(project(Projects.open.commons.kotlinCollections))
        api(project(Projects.open.commons.datetimeMinimal))
        api(libs.kotlinx.serialization.core)
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
        api(project(Projects.open.commons.time))
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))

        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))

        implementation(libs.controlsfx)

        implementation(libs.measured)
      }
    }
  }
}
