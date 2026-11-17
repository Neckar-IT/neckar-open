import it.neckar.gradle.Scope
import it.neckar.gradle.addKotlinTestDependencies

description = """OpenTelemetry Java Agent extension — derives resource attributes from the runtime-context environment.

Packaged as a self-contained OTel autoconfigure ResourceProvider. The resulting JAR is loaded two ways,
both from the single NeckarRuntimeContextResourceProvider source:
  - deployed services: as an OTel Java Agent extension (-Dotel.javaagent.extensions=...), the only way the
    agent picks up a ResourceProvider (application-classpath providers are NOT loaded by the agent).
  - IDE / integration-test runs (no agent): via the plain autoconfigured SDK, which does load
    application-classpath providers — consumers depend on this module so the provider is on their classpath.

Intentionally free of first-party dependencies: the agent loads the extension in an isolated classloader
that has neither the application classpath nor other JARs, only the agent's own (shaded) OpenTelemetry SDK.
Everything the provider needs must live in this JAR, so the runtime-context env keys are inlined here rather
than referenced from it.neckar.runtime.context.RuntimeContextEnv."""

plugins {
  `java-library`
  kotlinJvm
}

dependencies {
  // Provided at runtime by the OTel Java Agent (agent path) or by the consumer's OTel deps (non-agent path).
  compileOnly(libs.opentelemetry.api)
  compileOnly(libs.opentelemetry.sdk)
  compileOnly(libs.opentelemetry.sdk.extension.autoconfigure)

  addKotlinTestDependencies(project, Scope.Test)
  testImplementation(libs.opentelemetry.api)
  testImplementation(libs.opentelemetry.sdk.common)
  testImplementation(libs.opentelemetry.sdk.extension.autoconfigure.spi)
}
