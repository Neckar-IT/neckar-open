import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addKtorServerDependencies

description = """Generator for openapi.json files"""

plugins {
  `java-library`
  kotlinJvm
  kotlinxSerialization
  specHarvest
}

dependencies {
  api(project(Projects.open.commons.descriptors))

  addKtorServerDependencies(project, Scope.Main)

  api(project(Projects.open.commons.version))
  implementation(project(Projects.open.units.unit))
  implementation(project(Projects.open.commons.kotlinxSerializers))
  api(project(Projects.open.commons.kotlinLang))
  implementation(project(Projects.open.commons.kotlinCollections))
  api(project(Projects.open.ksp.specHarvestModel))

  api(project(Projects.open.rest.model))
  implementation(project(Projects.open.commons.kotlinReflect))

  api(libs.kotlinx.serialization.json)

  api(libs.ktor.http)
  api(libs.kotlinx.serialization.core)
  api(project(Projects.open.annotations))
  api(project(Projects.open.commons.lifeCycle))
  implementation(libs.ktor.utils)
  implementation(libs.kotlin.reflect)

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.user))
  testImplementation(project(Projects.open.commons.adjustable))
  testImplementation(project(Projects.open.commons.applicationError))
  testImplementation(project(Projects.open.commons.geometry))
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(project(Projects.open.commons.kotlinxSerializationTestUtils))

  //Proof of concept: checks that the generated schemas are loadable JSON Schema documents
  testImplementation(libs.json.schema.validator)

  testRuntimeOnly(libs.ktor.client.apache)
  testRuntimeOnly(libs.ktor.client.cio)
  testRuntimeOnly(libs.ktor.client.okhttp)
  testRuntimeOnly(libs.ktor.client.serialization)
}
