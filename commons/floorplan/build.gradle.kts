import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """Floor-plan import - finds rooms (outline + name + area) in architect DXF exports and draws each floor's background"""

plugins {
  kotlinMultiPlatform
  kotlinxSerialization
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlinx.serialization.core)

        api(project(Projects.open.commons.dxf))
        api(project(Projects.open.commons.geometry))
        api(project(Projects.open.units.unit))
      }
    }

    commonTest {
      dependencies {
      }
    }

    jvmTest {
      dependencies {
      }
    }
  }
}
