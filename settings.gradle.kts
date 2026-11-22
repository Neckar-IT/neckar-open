// Read by the export of internal/open alone. verifyStandaloneIncludes keeps the includes equal to the internal/open modules of
// the root settings.gradle.kts, minus those the root build.gradle.kts leaves out.

import org.gradle.util.GradleVersion

rootProject.name = "neckar-open"

val minimumGradleVersion: String = providers.gradleProperty("minimumGradleVersion").get()
val buildplatformVersion: String = providers.gradleProperty("buildplatformVersion").get()

// The export carries the wrapper of the monorepo; without the check an older Gradle fails inside the Kotlin plugin,
// where the message names anything but the cause.
require(GradleVersion.current() >= GradleVersion.version(minimumGradleVersion)) {
  "neckar-open needs Gradle $minimumGradleVersion or newer, this is ${GradleVersion.current().version}."
}

pluginManagement {
  val foojayResolverVersion: String = providers.gradleProperty("foojayResolverVersion").get()

  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  // Not from the catalog: pluginManagement is evaluated before any catalog exists.
  plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version foojayResolverVersion
  }

  // it.neckar.open.settings and the registry `Projects`, which names `Projects.open.commons.kotlinLang` as `:commons:kotlin-lang`.
  includeBuild("project-registry")
}

plugins {
  // Resolves the JDK 25 toolchain the same way the monorepo does.
  id("org.gradle.toolchains.foojay-resolver-convention")
  // Puts the convention plugins of the build platform at buildplatformVersion on the classpath of every build script, so a
  // script applies them by id without a version, and sets group, version and it.neckar.base for every project.
  id("it.neckar.open.settings")
}

dependencyResolutionManagement {
  // The version catalog alone resolves here: every project holds the repositories of it.neckar.base.
  repositories {
    mavenCentral()
  }

  versionCatalogs {
    // The repository's only catalog, published by the build platform: same aliases as inside the checkout.
    create("libs") {
      from("it.neckar.buildplatform:version-catalog:$buildplatformVersion")
    }
  }
}

include(":annotations")
include(":auth-rest-api")
include(":business-domain")
include(":contact")
include(":customer")
include(":financial")
include(":user")
include(":demos:kotlin-js-demo")
include(":demos:kotlin-react-demo")
include(":demos:typescript-react-demo")
include(":commons:adjustable")
include(":commons:app")
include(":commons:application-error")
include(":commons:authorization")
include(":commons:bson")
include(":commons:concurrent")
include(":commons:container")
include(":commons:crypt")
include(":commons:csv")
include(":commons:datetime-minimal")
include(":commons:descriptors")
include(":commons:disposable")
include(":commons:docker")
include(":commons:docker-model")
include(":commons:domain")
include(":commons:dxf")
include(":commons:health")
include(":commons:exception-handling")
include(":commons:exception-handling-ui")
include(":commons:exceptions")
include(":commons:execution")
include(":commons:feature-flags")
include(":commons:file")
include(":commons:floorplan")
include(":commons:geometry")
include(":commons:guava")
include(":commons:heartbeat")
include(":commons:html")
include(":commons:i18n")
include(":commons:image")
include(":commons:inject")
include(":commons:io")
include(":commons:jackson")
include(":commons:javafx")
include(":commons:javafx-test-utils")
include(":commons:javafx3d")
include(":commons:kotlin-async")
include(":commons:kotlin-bytearray")
include(":commons:kotlin-collections")
include(":commons:kotlin-events")
include(":commons:kotlin-js")
include(":commons:kotlin-lang")
include(":commons:kotlin-python")
include(":commons:kotlin-react")
include(":commons:kotlin-react-redux")
include(":commons:kotlin-react-router")
include(":commons:kotlin-reflect")
include(":commons:kotlin-result")
include(":commons:kotlinx-html")
include(":commons:kotlinx-serialization-test-utils")
include(":commons:kotlinx-serializers")
include(":commons:license")
include(":commons:life-cycle")
include(":commons:logging")
include(":commons:mail")
include(":commons:math")
include(":commons:memory")
include(":commons:money")
include(":commons:multi-tenancy")
include(":commons:observable")
include(":commons:otel-agent-extension")
include(":commons:payment")
include(":commons:pdf-to-image")
include(":commons:playwright")
include(":commons:provider")
include(":commons:random")
include(":commons:repeating")
include(":commons:resource-loading")
include(":commons:script-money")
include(":commons:storage")
include(":commons:tags")
include(":commons:test-data")
include(":commons:test-utils")
include(":commons:time")
include(":commons:tracing")
include(":commons:typescript:typescript-otel-tracing")
include(":commons:typescript:typescript-react-auth")
include(":commons:typescript:typescript-react-error-handling")
include(":commons:typescript:typescript-react-i18n")
include(":commons:typescript:typescript-react-query")
include(":commons:typescript:typescript-react-router")
include(":commons:typescript:typescript-react-ui")
include(":commons:typescript:typescript-react-ui-primitives")
include(":commons:typescript:typescript-react-utils")
include(":commons:typescript:typescript-unit-tests")
include(":commons:typescript:typescript-utils")
include(":commons:user-id")
include(":commons:uuid")
include(":commons:version")
include(":commons:workflow")
include(":commons:xml-commons")
include(":commons:zip")
include(":config:typescript:orval-config")
include(":config:typescript:oxfmt-config")
include(":config:typescript:oxlint-config")
include(":config:typescript:oxlint-plugin")
include(":config:typescript:playwright-config")
include(":config:typescript:tailwind-config")
include(":config:typescript:typescript-config")
include(":config:typescript:vite-config")
include(":detekt")
include(":integrations:mattermost-client-api")
include(":ksp")
include(":ksp:boxing-verifier")
include(":ksp:ksp-commons")
include(":ksp:ksp-test-utils")
include(":ksp:serialization-verifier")
include(":ksp:spec-harvest")
include(":ksp:spec-harvest-model")
include(":ksp:ts-declaration-generator")
include(":ksp:types-list-generator")
include(":rest:ktor-client")
include(":rest:ktor-client-test-utils")
include(":rest:ktor-server")
include(":rest:ktor-server-mcp")
include(":rest:ktor-server-test-utils")
include(":rest:model")
include(":rest:model-health")
include(":rest:model-paging")
include(":rest:openapi-generator")
include(":units:unit-conversion")
include(":units:unit")
include(":units:unit-utils")
include(":version-info")
include(":web-resources")
include(":web-resources:web-fonts")
