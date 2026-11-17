import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies

description = """DXF parser - reads header, layers, linetypes, blocks and entities from Autodesk DXF files"""

plugins {
  kotlinMultiPlatform
}

kotlin {
  addAnnotationDependencies(project)
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
  }
}
