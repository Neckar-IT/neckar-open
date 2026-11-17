import it.neckar.gradle.addKotlinTestDependencies

import it.neckar.gradle.addAnnotationDependencies
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

plugins {
}

description = """Detekt Rules"""


dependencies {
  addAnnotationDependencies(project)
  addKotlinTestDependencies(project)

  components {
    // Detekt 2.0.0-alpha.4, alpha.5 and alpha.6 all ship a broken publication: detekt-test's runtimeElements
    // declares a dependency on detekt-api carrying the `detekt-api-test-fixtures` capability, but the
    // matching binary jar (detekt-api-<version>-test-fixtures.jar) was never uploaded to Maven Central
    // — only the -test-fixtures-sources.jar exists. Resolving this module's testRuntimeClasspath
    // therefore fails with "No matching variant of dev.detekt:detekt-api was found …
    // detekt-api-test-fixtures". The three fixtures classes (dev.detekt.api.testfixtures.{TestDetektion,
    // TestFactory,TestSetupContext}) are only referenced by detekt-test's
    // FileProcessListenerExtensionsKt, which the test helpers we use (lint / lintWithContext /
    // createEnvironment / Config) never load — so satisfying the capability from the main detekt-api
    // jar is sufficient and loads no broken class.
    // workaround: dependency=libs:dev-detekt:2.0.0-alpha.6 observed-on=2026-10-07 upstream=https://github.com/detekt/detekt/issues/9409
    // The rule goes once a detekt release publishes the binary test-fixtures jar.
    withModule<DetektApiTestFixturesCapabilityRule>("dev.detekt:detekt-api")
  }

  // Do NOT add kotlin-compiler-embeddable here: it conflicts with the shaded PSI
  // that Detekt 2.0 bundles, causing NoSuchMethodError at runtime.
  api(libs.detekt.api)

  // detekt-test transitively pulls detekt-test-utils + kotlin-compiler. The conflict with the
  // shaded PSI only affects detektPlugins consumers at detekt-runtime; the test classpath of
  // this module is independent.
  testImplementation(libs.detekt.test)
}

/**
 * Adds the `detekt-api-test-fixtures` capability to detekt-api's regular jar variants so the
 * capability detekt-test requests resolves to the main artifact. Works around the missing
 * test-fixtures jar in the detekt 2.0.0-alpha.4/alpha.5/alpha.6 publication (see the [components] block above).
 */
abstract class DetektApiTestFixturesCapabilityRule : ComponentMetadataRule {
  override fun execute(context: ComponentMetadataContext) {
    val version = context.details.id.version
    listOf("apiElements", "runtimeElements").forEach { variantName ->
      context.details.withVariant(variantName) {
        withCapabilities {
          addCapability("dev.detekt", "detekt-api-test-fixtures", version)
        }
      }
    }
  }
}

// This module cannot run detekt on itself: the custom neckar-rules ruleset lives here and is
// loaded as a detektPlugins dep by every other project — not by this one (would be a circular
// reference). Without the plugin on the classpath, detekt rejects the neckar-rules section in
// internal/buildplatform/config/detekt/detekt.yml. Disable the self-check instead; every consumer of the rule still
// runs detekt normally.
tasks.matching { it.name.startsWith("detekt") }.configureEach {
  enabled = false
}
