import it.neckar.gradle.addKotlinTestDependencies
import it.neckar.projects.Projects
import it.neckar.projects.project

description = """Meistercharts - History Core"""

plugins {
  kotlinxSerialization
}

repositories {
  mavenCentral()
}

kotlin {
  sourceSets {
    addKotlinTestDependencies(project)

    commonMain {
      dependencies {
        // workaround: dependency=libs:kotlin:2.4.20 observed-on=2026-10-07 upstream=https://youtrack.jetbrains.com/issue/KT-43500
        // compileOnly is unsupported for Kotlin/JS: Kotlin 2.4.20 reports IncorrectCompileOnlyDependencyWarning.
        implementation(project(Projects.open.annotations))

        api(project(Projects.open.units.unit))
        api(project(Projects.open.commons.provider))
        api(project(Projects.open.commons.disposable))
        api(project(Projects.open.meistercharts.meisterchartsCore))

        api(project(Projects.open.commons.i18n))
        api(project(Projects.open.commons.kotlinCollections))

        //Required for serializers
        api(project(Projects.open.commons.kotlinxSerializers))

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
        api(project(Projects.open.annotations))

        api(libs.threeten.extra)
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(project(Projects.open.commons.kotlinxSerializationTestUtils))
      }
    }
  }
}
