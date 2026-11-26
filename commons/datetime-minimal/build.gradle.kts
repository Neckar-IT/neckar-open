import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies
import it.neckar.gradle.registerMultiplatformPerformanceTest

// Proleptic gregorian calendar from the Big Bang to year 15,000,000,000, computed in common code;
// the platform only supplies the time zone offset (java.time, Temporal, Intl).

plugins {
  kotlinxSerialization
  generateTsDeclaration
}

description = "Minimal multiplatform date and time value types (LocalDate, LocalTime, LocalDateTime, TimeZone) with no platform dependencies."

kotlin {
  js {
    nodejs {
      testTask {
        // TemporalParityTest compares 1.3 million days with Temporal.PlainDate.
        useMocha { timeout = "120s" }
      }
    }
  }

  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.annotations))
        api(project(Projects.open.units.unit))
        implementation(project(Projects.open.commons.kotlinCollections))
        implementation(libs.kotlinx.serialization.core)
      }
    }

    commonTest {
      dependencies {

        implementation(libs.kotlinx.datetime) //Only for tests! To verify the minimal implementation
      }
    }

    jsMain {
      dependencies {
      }
    }

    jsTest {
      dependencies {
        implementation(libs.kotlin.test.js)
      }
    }

    jvmMain {
      dependencies {
        api(project(Projects.open.annotations))
      }
    }

    jvmTest {
      dependencies {
        implementation(project(Projects.open.commons.testUtils))
        implementation(libs.kotlinx.datetime) // verify serialization parity against the minimal impl
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}

// Measures run time and allocations: `./gradlew :internal:open:commons:datetime-minimal:performanceTest`
registerMultiplatformPerformanceTest("performanceTest")
