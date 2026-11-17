import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Mail"""

plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  api(libs.mail)
  api(libs.slf4j.api)

  addKotlinTestDependencies(project)
}
