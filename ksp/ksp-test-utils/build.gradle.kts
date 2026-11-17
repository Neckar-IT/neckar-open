import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
plugins {
}

description = "Test utilities for Kotlin Symbol Processing (KSP) processors."

dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  api(libs.symbol.processing.api)
  api(libs.assertk)

  implementation(libs.dev.zacsweers.kctfork.core)
  implementation(libs.ksp) //zacsweers
  implementation(libs.kotlin.compiler.embeddable)
}
