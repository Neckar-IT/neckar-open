import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import it.neckar.gradle.addKtorClientDependencies
import it.neckar.gradle.addKtorServerDependencies

description = """Ktor Server Test Utils"""

plugins {
  `java-library`
  kotlinJvm
  kotlinxSerialization
  openapiGenerationConfig
}

dependencies {
  addAnnotationDependencies(project)
  addKtorServerDependencies(project, Scope.Main)

  api(project(Projects.open.rest.model))
  api(project(Projects.open.rest.ktorServer))
  api(libs.ktor.server.test.host)

  addKtorClientDependencies(project, Scope.Main)
  implementation(project(Projects.open.rest.ktorClient))
  implementation(project(Projects.open.versionInfo))
  // RestAssertions + route/ endpoint-introspection helpers live here (they don't depend on ktor-server);
  // re-exported so existing consumers keep transitive access without a build-graph cycle.
  api(project(Projects.open.rest.ktorClientTestUtils))

  runtimeOnly(libs.jackson.module.kotlin)
  runtimeOnly(libs.jackson.module.parameter.names)
  runtimeOnly(libs.jackson.datatype.jdk8)
  runtimeOnly(libs.jackson.datatype.jsr310)

  api(libs.kotlinx.serialization.json)

  api(libs.java.jwt)
  api(libs.ktor.http)
  api(libs.ktor.utils)
  api(libs.kotlinx.serialization.core)
  api(libs.junit.jupiter.api)
  api(project(Projects.open.user))
  api(project(Projects.open.rest.openapiGenerator))
  api(project(Projects.open.commons.multiTenancy))
  implementation(libs.ktor.serialization)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.junit.platform.commons)
  implementation(libs.kotlin.reflect)

  addKotlinTestDependencies(project)
  api(project(Projects.open.commons.testUtils))
  api(project(Projects.open.commons.kotlinLang))

  testImplementation(libs.ktor.server.default.headers)
  testImplementation(libs.slf4j.api)
  testImplementation(project(Projects.open.commons.logging))
  testImplementation(project(Projects.open.commons.uuid))
  testImplementation(project(Projects.open.commons.version))
}
