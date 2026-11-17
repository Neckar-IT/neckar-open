import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKotlinDependencies

// Minimal, multiplatform calendar/time value types (LocalDate/LocalTime/LocalDateTime/TimeZone).
//
// USE THIS ONLY WHERE A Kotlin/JS TARGET IS INVOLVED (types shared into a Kotlin/JS frontend).
// It exists purely because kotlinx-datetime historically was awkward on JS; it is deliberately
// bare ("without any special checks") and has known rough edges (e.g. the JVM
// fromMillisCurrentTimeZone system-timezone guard, "time zone ignored on JS").
//
// For JVM-only code (Ktor services with a TypeScript frontend, CLIs, migrations) PREFER
// kotlinx.datetime — it has proper, tested timezone handling and serializes to the same ISO
// wire form. See the KDoc on LocalDate for details.

plugins {
  kotlinxSerialization
  generateTsDeclaration
}

description = "Minimal multiplatform date and time value types (LocalDate, LocalTime, LocalDateTime, TimeZone) with no platform dependencies."

kotlin {
  addAnnotationDependencies(project)
  addKotlinDependencies(project)
  addKotlinTestDependencies(project)

  sourceSets {
    commonMain {
      dependencies {
        api(project(Projects.open.commons.kotlinLang))
        api(project(Projects.open.annotations))
        api(project(Projects.open.units.unit))
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
        api(project(Projects.open.commons.time))
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
