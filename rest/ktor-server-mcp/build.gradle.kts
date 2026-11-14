import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addKtorClientDependencies
import it.neckar.gradle.addKtorServerDependencies

description = """Ktor Server MCP Endpoint"""

plugins {
  `java-library`
  kotlinJvm
  kotlinxSerialization
  openapiGenerationConfig
}

dependencies {
  addKtorServerDependencies(project, Scope.Main)

  api(project(Projects.open.rest.ktorServer))
  api(project(Projects.open.commons.descriptors))
  api(libs.modelcontextprotocol.kotlin.sdk.server)
  implementation(project(Projects.open.versionInfo))
  implementation(libs.ktor.serialization)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.ktor.server.body.limit)

  addKotlinTestDependencies(project)
  addKtorClientDependencies(project, Scope.Test)
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(project(Projects.open.rest.ktorServerTestUtils))
  testImplementation(libs.modelcontextprotocol.kotlin.sdk.client)
}
