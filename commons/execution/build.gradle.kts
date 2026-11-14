import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies

description = """Process execution stuff"""


plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  addAnnotationDependencies(project)

  implementation(project(Projects.open.commons.kotlinLang))

  addKotlinTestDependencies(project)
  testImplementation(libs.guava)
  testImplementation(project(Projects.open.commons.testUtils))
}
