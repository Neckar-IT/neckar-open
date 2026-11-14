import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

plugins {
  kotlinxSerialization
}

description = "Data model for the specifications harvested by the spec-harvest KSP processor."

dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  api(libs.kotlinx.serialization.core)

  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.kotlinx.serialization.core)
}
