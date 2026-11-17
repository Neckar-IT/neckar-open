import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addKotlinDependencies

description = """Docker"""


plugins {
  `java-library`
  kotlinJvm
  kotlinxSerialization
}

dependencies {
  compileOnlyApi(project(Projects.open.annotations))
  addKotlinDependencies()

  api(project(Projects.open.commons.concurrent))
  api(project(Projects.open.commons.disposable))
  api(project(Projects.open.commons.kotlinCollections))
  api(project(Projects.open.commons.i18n))
  api(project(Projects.open.commons.logging))

  implementation(project(Projects.open.rest.ktorClient))

  api(project(Projects.open.commons.dockerModel))

  //Docker support
  api(libs.docker.java.api)
  api(libs.docker.java.core)
  api(libs.docker.java.transport.httpclient5)

  addKotlinTestDependencies(project)
  testImplementation(libs.jfxtras.all)
  testImplementation(libs.controlsfx)
  testImplementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.kotlinx.coroutines.javafx)
}

