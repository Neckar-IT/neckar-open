import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """Money / currency value-types (Money, MoneyFraction, VatRate, PricePerHour, ...)"""

plugins {
  kotlinxSerialization
  generateTypesList
}

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.annotations))

        implementation(libs.kotlinx.serialization.core)

        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.commons.i18n))
        api(project(Projects.open.commons.kotlinxSerializers))
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
        implementation(libs.dev.zacsweers.kctfork.core) //MoneyTypeSafetyCompileTest
        implementation(libs.kotlin.compiler.embeddable) //MoneyTypeSafetyCompileTest
      }
    }
  }
}
