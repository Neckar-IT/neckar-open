import it.neckar.projects.project

import it.neckar.projects.Projects

import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addKtorServerDependencies

description = """Ktor Server Commons"""

plugins {
  `java-library`
  kotlinJvm
  kotlinxSerialization
  openapiGenerationConfig
}

dependencies {
  api(project(Projects.open.commons.descriptors))

  addKtorServerDependencies(project, Scope.Main)

  // CspNonce lives beside HtmlInlineSupport: inlining creates the blocks the nonce marks.
  api(project(Projects.open.commons.html))
  api(project(Projects.open.commons.version))
  api(project(Projects.open.units.unit))
  api(project(Projects.open.commons.uuid))
  implementation(project(Projects.open.commons.kotlinxSerializers))
  api(project(Projects.open.commons.featureFlags))
  api(project(Projects.open.commons.kotlinLang))
  implementation(project(Projects.open.commons.kotlinCollections))
  implementation(project(Projects.open.commons.logging))
  implementation(project(Projects.open.commons.applicationError))
  api(project(Projects.open.user))
  api(project(Projects.open.commons.authorization))
  api(project(Projects.open.commons.multiTenancy))
  api(project(Projects.open.rest.openapiGenerator))

  api(project(Projects.open.rest.model))
  api(project(Projects.open.commons.health))

  // OTel ResourceProvider (deployment.environment etc. from the runtime-context env). On the
  // application classpath here it covers non-agent runs (IDE/test); deployed services additionally
  // load the same class as an OTel Java Agent extension baked into the image (OtelAgentPlugin).
  api(project(Projects.open.commons.otelAgentExtension))

  api(libs.ktor.server.auth.jwt)
  api(libs.ktor.server.forwarded.header)

  api(libs.kotlinx.serialization.json)

  api(libs.micrometer.registry.prometheus)

  // OpenTelemetry library instrumentation
  api(libs.opentelemetry.api)
  api(libs.opentelemetry.sdk)
  api(libs.opentelemetry.sdk.extension.autoconfigure)
  api(libs.opentelemetry.exporter.otlp)
  api(libs.opentelemetry.ktor.v3)
  api(libs.opentelemetry.extension.kotlin)
  implementation(libs.opentelemetry.semconv)

  api(libs.java.jwt)
  api(libs.ktor.http)
  api(libs.ktor.server.call.logging)
  api(libs.ktor.server.default.headers)
  api(libs.ktor.server.content.negotiation)
  api(libs.ktor.server.cors)
  api(libs.ktor.server.status.pages)
  api(libs.ktor.utils)
  api(libs.kotlinx.serialization.core)
  api(libs.slf4j.api)
  api(project(Projects.open.annotations))
  implementation(libs.ktor.serialization)
  implementation(libs.ktor.server.compression)
  implementation(libs.micrometer.core)
  implementation(project(Projects.open.commons.exceptions))
  implementation(project(Projects.open.commons.repeating))
  implementation(project(Projects.open.commons.time))
  implementation(project(Projects.open.versionInfo))

  addKotlinTestDependencies(project)
  testImplementation(project(Projects.open.commons.testUtils))
  testImplementation(project(Projects.open.commons.kotlinxSerializationTestUtils))

  testImplementation(project(Projects.open.rest.ktorClient))
  testImplementation(project(Projects.open.rest.ktorClientTestUtils))

  testImplementation(libs.ktor.client.content.negotiation)

  testImplementation(libs.opentelemetry.sdk.testing)
}

