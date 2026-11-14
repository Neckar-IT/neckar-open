import it.neckar.projects.project


import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """IO"""


plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  implementation(project(Projects.open.commons.kotlinLang))
  implementation(libs.commons.io)

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(project(Projects.open.commons.file))
  testImplementation(libs.guava)
}
