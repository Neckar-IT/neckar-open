import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """kotlin-result - Result monad for modelling success or failure operations"""

plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  api(libs.kotlin.result)

  addKotlinTestDependencies(project)
}
