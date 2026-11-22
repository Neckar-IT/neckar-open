import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """Meistercharts - Canvas"""

plugins {
  generateIcons
  kotlinxSerialization
  kspBoxing
  generateTypesList
  generateTsDeclaration
}

kotlin {
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {

        api(project(Projects.open.annotations))
        api(project(Projects.open.units.unit))
        api(project(Projects.open.units.unitConversion))
        api(project(Projects.open.commons.kotlinAsync))
        api(project(Projects.open.commons.logging))
        api(project(Projects.open.commons.kotlinJs))

        api(project(Projects.open.meistercharts.meisterchartsCore))

        //TODO remove deps - or make optional
        api(project(Projects.open.meistercharts.meisterchartsHistory.meisterchartsHistoryCore))
        api(project(Projects.open.meistercharts.meisterchartsHistory.meisterchartsHistoryApi))
        api(project(Projects.open.commons.kotlinEvents))
        api(project(Projects.open.meistercharts.meisterchartsVersionInfo))
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlinx.html)
      }
    }

    jsMain {
      dependencies {
      }
    }

    webMain {
      dependencies {
        implementation(libs.kotlinx.browser)
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
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))

        implementation(libs.controlsfx)
      }
    }
  }
}

//tasks.register("sourcesJar") {
//  group = "Build"
//  description = "Assembles the sources jar for JS and JVM"
//
//  dependsOn("jsSourcesJar")
//  dependsOn("jvmSourcesJar")
//}

createIcons {
  objectName = "Icons"

  svgIconsSourceDir = file("src/icons/")
  basicIconsFile = file("src/commonMain/kotlin/com/meistercharts/resources/Icons.kt")

  svgPathsFile = file("src/commonMain/kotlin/com/meistercharts/resources/svg/SvgPaths.kt")
  svgPaintableProvidersFile = file("src/commonMain/kotlin/com/meistercharts/resources/svg/SvgPaintableProviders.kt")
  svgPaintablesProviderProviderFile = file("src/commonMain/kotlin/com/meistercharts/resources/svg/SvgPaintablesProviderProvider.kt")
}
