package it.neckar.projects

import it.neckar.projects.KotlinTarget.Js
import it.neckar.projects.KotlinTarget.Jvm
import it.neckar.projects.KotlinTarget.LinuxX64
import it.neckar.projects.KotlinTarget.WasmJs

/**
 * The projects under `internal/open`. The monorepo registers them below `:internal:open`, a build rooted at
 * `internal/open` below `:`.
 */
class OpenProjects(
  parent: ConfiguredProject,
  relativePath: GradleProjectPath,
  /** The full path of [meistercharts]: `:` in the export of `internal/open/meistercharts`, whose root project is MeisterCharts. */
  meisterchartsPath: GradleProjectPath = parent.path.resolve(relativePath).resolve(MeisterchartsDirectory),
) : ConfiguredProject(parent, relativePath, ProjectType.Intermediate, ProjectRole.Container) {
  companion object {
    /** This file, relative to `internal/open`. */
    const val RegistryFile: String = "project-registry/open-projects/src/main/kotlin/it/neckar/projects/OpenProjects.kt"

    private val MeisterchartsDirectory: GradleProjectPath = GradleProjectPath(":meistercharts")
  }

  val annotations: ConfiguredProject = multiplatform("annotations", ProjectRole.Library, Jvm, Js, WasmJs, LinuxX64)
  val authRestApi: ConfiguredProject = multiplatform("auth-rest-api", ProjectRole.Library, Jvm, Js)
  val businessDomain: ConfiguredProject = multiplatform("business-domain", ProjectRole.Library, Jvm, Js)
  val commons: Commons = Commons(this)
  val config: Config = Config(this)
  val contact: ConfiguredProject = multiplatform("contact", ProjectRole.Library, Jvm, Js, WasmJs)
  val customer: ConfiguredProject = multiplatform("customer", ProjectRole.Library, Jvm, Js)
  val demos: Demos = Demos(this)
  val detekt: ConfiguredProject = jvm("detekt", ProjectRole.Library)
  val financial: ConfiguredProject = multiplatform("financial", ProjectRole.Library, Jvm, Js)
  val integrations: Integrations = Integrations(this)
  val ksp: Ksp = Ksp(this)
  val meistercharts: MeisterchartsProjects = MeisterchartsProjects(this, MeisterchartsDirectory, meisterchartsPath)
  val rest: Rest = Rest(this)
  val units: Units = Units(this)
  val user: ConfiguredProject = multiplatform("user", ProjectRole.Library, Jvm, Js)
  val versionInfo: ConfiguredProject = multiplatform("version-info", ProjectRole.Library, Jvm, Js, WasmJs)
  val webResources: WebResources = WebResources(this)

  /**
   * Projects that are allowed as dependencies in ksp projects.
   */
  val allowedDependenciesForKspProcessingProjects: List<ConfiguredProject>
    get() = listOf(annotations, units.unit)

  class Commons(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":commons"), ProjectType.Intermediate, ProjectRole.Container) {
    val adjustable: ConfiguredProject = multiplatform("adjustable", ProjectRole.Library, Jvm, Js, WasmJs)
    val app: ConfiguredProject = multiplatform("app", ProjectRole.Library, Jvm, LinuxX64)
    val applicationError: ConfiguredProject = multiplatform("application-error", ProjectRole.Library, Jvm, Js, WasmJs)
    val authorization: ConfiguredProject = multiplatform("authorization", ProjectRole.Library, Jvm, Js)
    val bson: ConfiguredProject = jvm("bson", ProjectRole.Library)
    val concurrent: ConfiguredProject = multiplatform("concurrent", ProjectRole.Library, Jvm, Js, WasmJs)
    val container: ConfiguredProject = multiplatform("container", ProjectRole.Library, Jvm, LinuxX64)
    val containerCli: ConfiguredProject = multiplatform("container-cli", ProjectRole.Tool, LinuxX64)
    val crypt: ConfiguredProject = multiplatform("crypt", ProjectRole.Library, Jvm, Js)
    val csv: ConfiguredProject = multiplatform("csv", ProjectRole.Library, Jvm, Js, WasmJs)
    val datetimeMinimal: ConfiguredProject = multiplatform("datetime-minimal", ProjectRole.Library, Jvm, Js, WasmJs)
    val descriptors: ConfiguredProject = multiplatform("descriptors", ProjectRole.Library, Jvm, Js, WasmJs)
    val disposable: ConfiguredProject = multiplatform("disposable", ProjectRole.Library, Jvm, Js, WasmJs)
    val docker: ConfiguredProject = jvm("docker", ProjectRole.Library)
    val dockerModel: ConfiguredProject = jvm("docker-model", ProjectRole.Library)
    val domain: ConfiguredProject = multiplatform("domain", ProjectRole.Library, Jvm, Js)
    val dxf: ConfiguredProject = multiplatform("dxf", ProjectRole.Library, Jvm, Js)
    val exceptionHandling: ConfiguredProject = jvm("exception-handling", ProjectRole.Library)
    val exceptionHandlingUi: ConfiguredProject = jvm("exception-handling-ui", ProjectRole.Library)
    val exceptions: ConfiguredProject = multiplatform("exceptions", ProjectRole.Library, Jvm, Js, WasmJs)
    val execution: ConfiguredProject = jvm("execution", ProjectRole.Library)
    val featureFlags: ConfiguredProject = multiplatform("feature-flags", ProjectRole.Library, Jvm, Js, WasmJs)
    val file: ConfiguredProject = jvm("file", ProjectRole.Library)
    val floorplan: ConfiguredProject = multiplatform("floorplan", ProjectRole.Library, Jvm, Js)
    val geometry: ConfiguredProject = multiplatform("geometry", ProjectRole.Library, Jvm, Js, WasmJs)
    val guava: ConfiguredProject = jvm("guava", ProjectRole.Library)
    val health: ConfiguredProject = multiplatform("health", ProjectRole.Library, Jvm)
    val heartbeat: ConfiguredProject = multiplatform("heartbeat", ProjectRole.Library, Jvm, Js, WasmJs)
    val html: ConfiguredProject = jvm("html", ProjectRole.Library)
    val i18n: ConfiguredProject = multiplatform("i18n", ProjectRole.Library, Jvm, Js, WasmJs)
    val image: ConfiguredProject = jvm("image", ProjectRole.Library)
    val inject: ConfiguredProject = jvm("inject", ProjectRole.Library)
    val io: ConfiguredProject = jvm("io", ProjectRole.Library)
    val jackson: ConfiguredProject = jvm("jackson", ProjectRole.Library)
    val javafx: ConfiguredProject = jvm("javafx", ProjectRole.Library)
    val javafxTestUtils: ConfiguredProject = jvm("javafx-test-utils", ProjectRole.Library)
    val javafx3d: ConfiguredProject = jvm("javafx3d", ProjectRole.Library)
    val kotlinAsync: ConfiguredProject = multiplatform("kotlin-async", ProjectRole.Library, Jvm, Js, WasmJs)
    val kotlinBytearray: ConfiguredProject = multiplatform("kotlin-bytearray", ProjectRole.Library, Jvm, Js, WasmJs)
    val kotlinCollections: ConfiguredProject = multiplatform("kotlin-collections", ProjectRole.Library, Jvm, Js, WasmJs)
    val kotlinEvents: ConfiguredProject = multiplatform("kotlin-events", ProjectRole.Library, Jvm, Js, WasmJs)
    val kotlinJs: ConfiguredProject = multiplatform("kotlin-js", ProjectRole.Library, Jvm, Js, WasmJs)
    val kotlinLang: ConfiguredProject = multiplatform("kotlin-lang", ProjectRole.Library, Jvm, Js, WasmJs)
    val kotlinPython: ConfiguredProject = jvm("kotlin-python", ProjectRole.Library)
    val kotlinReact: ConfiguredProject = multiplatform("kotlin-react", ProjectRole.Library, Jvm, Js)
    val kotlinReactRedux: ConfiguredProject = multiplatform("kotlin-react-redux", ProjectRole.Library, Jvm, Js)
    val kotlinReactRouter: ConfiguredProject = multiplatform("kotlin-react-router", ProjectRole.Library, Jvm, Js)
    val kotlinReflect: ConfiguredProject = multiplatform("kotlin-reflect", ProjectRole.Library, Jvm, Js)
    val kotlinResult: ConfiguredProject = jvm("kotlin-result", ProjectRole.Library)
    val kotlinxHtml: ConfiguredProject = multiplatform("kotlinx-html", ProjectRole.Library, Jvm, Js)
    val kotlinxSerializationTestUtils: ConfiguredProject = jvm("kotlinx-serialization-test-utils", ProjectRole.Library)
    val kotlinxSerializers: ConfiguredProject = multiplatform("kotlinx-serializers", ProjectRole.Library, Jvm, Js, WasmJs)
    val license: ConfiguredProject = jvm("license", ProjectRole.Library)
    val lifeCycle: ConfiguredProject = multiplatform("life-cycle", ProjectRole.Library, Jvm, Js, WasmJs)
    val logging: ConfiguredProject = multiplatform("logging", ProjectRole.Library, Jvm, Js, WasmJs)
    val mail: ConfiguredProject = jvm("mail", ProjectRole.Library)
    val math: ConfiguredProject = multiplatform("math", ProjectRole.Library, Jvm, Js, WasmJs)
    val memory: ConfiguredProject = jvm("memory", ProjectRole.Library)
    val money: ConfiguredProject = multiplatform("money", ProjectRole.Library, Jvm, Js, WasmJs)
    val multiTenancy: ConfiguredProject = multiplatform("multi-tenancy", ProjectRole.Library, Jvm, Js, WasmJs)
    val observable: ConfiguredProject = multiplatform("observable", ProjectRole.Library, Jvm, Js, WasmJs)
    val otelAgentExtension: ConfiguredProject = jvm("otel-agent-extension", ProjectRole.Library)
    val payment: ConfiguredProject = multiplatform("payment", ProjectRole.Library, Jvm, Js)
    val pdfToImage: ConfiguredProject = jvm("pdf-to-image", ProjectRole.Library)
    val playwright: ConfiguredProject = jvm("playwright", ProjectRole.Library)
    val provider: ConfiguredProject = multiplatform("provider", ProjectRole.Library, Jvm, Js, WasmJs)
    val random: ConfiguredProject = multiplatform("random", ProjectRole.Library, Jvm, Js, WasmJs)
    val repeating: ConfiguredProject = multiplatform("repeating", ProjectRole.Library, Jvm, Js, WasmJs)
    val resourceLoading: ConfiguredProject = multiplatform("resource-loading", ProjectRole.Library, Jvm, Js, WasmJs)
    val scriptMoney: ConfiguredProject = pnpm("script-money", ProjectRole.Library)
    val storage: ConfiguredProject = multiplatform("storage", ProjectRole.Library, Jvm, Js)
    val tags: ConfiguredProject = multiplatform("tags", ProjectRole.Library, Jvm, Js)
    val testData: ConfiguredProject = multiplatform("test-data", ProjectRole.Library, Jvm, Js)
    val testUtils: ConfiguredProject = multiplatform("test-utils", ProjectRole.Library, Jvm, Js)
    val time: ConfiguredProject = multiplatform("time", ProjectRole.Library, Jvm, Js, WasmJs)
    val tracing: ConfiguredProject = multiplatform("tracing", ProjectRole.Library, Jvm, Js, WasmJs)
    val typescript: Typescript = Typescript(this)
    val userId: ConfiguredProject = multiplatform("user-id", ProjectRole.Library, Jvm, Js, WasmJs)
    val uuid: ConfiguredProject = multiplatform("uuid", ProjectRole.Library, Jvm, Js, WasmJs)
    val version: ConfiguredProject = multiplatform("version", ProjectRole.Library, Jvm, Js, WasmJs, LinuxX64)
    val workflow: ConfiguredProject = multiplatform("workflow", ProjectRole.Library, Jvm, Js)
    val xmlCommons: ConfiguredProject = jvm("xml-commons", ProjectRole.Library)
    val zip: ConfiguredProject = jvm("zip", ProjectRole.Library)

    class Typescript(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":typescript"), ProjectType.Intermediate, ProjectRole.Container) {
      val typescriptOtelTracing: ConfiguredProject = pnpm("typescript-otel-tracing", ProjectRole.Library)
      val typescriptReactAuth: ConfiguredProject = pnpm("typescript-react-auth", ProjectRole.Library)
      val typescriptReactErrorHandling: ConfiguredProject = pnpm("typescript-react-error-handling", ProjectRole.Library)
      val typescriptReactI18n: ConfiguredProject = pnpm("typescript-react-i18n", ProjectRole.Library)
      val typescriptReactQuery: ConfiguredProject = pnpm("typescript-react-query", ProjectRole.Library)
      val typescriptReactRouter: ConfiguredProject = pnpm("typescript-react-router", ProjectRole.Library)
      val typescriptReactUi: ConfiguredProject = pnpm("typescript-react-ui", ProjectRole.Library)
      val typescriptReactUiPrimitives: ConfiguredProject = pnpm("typescript-react-ui-primitives", ProjectRole.Library)
      val typescriptReactUtils: ConfiguredProject = pnpm("typescript-react-utils", ProjectRole.Library)
      val typescriptUnitTests: ConfiguredProject = pnpm("typescript-unit-tests", ProjectRole.TestSuite)
      val typescriptUtils: ConfiguredProject = pnpm("typescript-utils", ProjectRole.Library)
    }
  }

  class Config(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":config"), ProjectType.Intermediate, ProjectRole.Container) {
    val typescript: Typescript = Typescript(this)

    class Typescript(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":typescript"), ProjectType.Intermediate, ProjectRole.Container) {
      val orvalConfig: ConfiguredProject = pnpm("orval-config", ProjectRole.Library)
      val oxfmtConfig: ConfiguredProject = pnpm("oxfmt-config", ProjectRole.Library)
      val oxlintConfig: ConfiguredProject = pnpm("oxlint-config", ProjectRole.Library)
      val oxlintPlugin: ConfiguredProject = pnpm("oxlint-plugin", ProjectRole.Library)
      val playwrightConfig: ConfiguredProject = pnpm("playwright-config", ProjectRole.Library)
      val tailwindConfig: ConfiguredProject = pnpm("tailwind-config", ProjectRole.Library)
      val typescriptConfig: ConfiguredProject = pnpm("typescript-config", ProjectRole.Library)
      val viteConfig: ConfiguredProject = pnpm("vite-config", ProjectRole.Library)
    }
  }

  class Demos(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":demos"), ProjectType.Intermediate, ProjectRole.Container) {
    val kotlinJsDemo: ConfiguredProject = multiplatform("kotlin-js-demo", ProjectRole.Frontend, Jvm, Js)
    val kotlinReactDemo: ConfiguredProject = multiplatform("kotlin-react-demo", ProjectRole.Frontend, Jvm, Js)
    val typescriptReactDemo: ConfiguredProject = vite("typescript-react-demo", ProjectRole.Frontend)
  }

  class Integrations(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":integrations"), ProjectType.Intermediate, ProjectRole.Container) {
    val mattermostClientApi: ConfiguredProject = multiplatform("mattermost-client-api", ProjectRole.Library, Jvm, Js)
  }

  class Ksp(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":ksp"), ProjectType.Intermediate, ProjectRole.Container) {
    val boxingVerifier: ConfiguredProject = kspProcessor("boxing-verifier", ProjectRole.Library)
    val kspCommons: ConfiguredProject = kspProcessor("ksp-commons", ProjectRole.Library)
    val kspTestUtils: ConfiguredProject = kspProcessor("ksp-test-utils", ProjectRole.Library)
    val serializationVerifier: ConfiguredProject = kspProcessor("serialization-verifier", ProjectRole.Library)
    val specHarvest: ConfiguredProject = kspProcessor("spec-harvest", ProjectRole.Library)
    val specHarvestModel: ConfiguredProject = jvm("spec-harvest-model", ProjectRole.Library)
    val tsDeclarationGenerator: ConfiguredProject = kspProcessor("ts-declaration-generator", ProjectRole.Library)
    val typesListGenerator: ConfiguredProject = kspProcessor("types-list-generator", ProjectRole.Library)
  }

  class Rest(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":rest"), ProjectType.Intermediate, ProjectRole.Container) {
    val ktorClient: ConfiguredProject = multiplatform("ktor-client", ProjectRole.Library, Jvm, Js, WasmJs)
    val ktorClientTestUtils: ConfiguredProject = multiplatform("ktor-client-test-utils", ProjectRole.Library, Jvm, Js)
    val ktorServer: ConfiguredProject = jvm("ktor-server", ProjectRole.Library)
    val ktorServerMcp: ConfiguredProject = jvm("ktor-server-mcp", ProjectRole.Library)
    val ktorServerTestUtils: ConfiguredProject = jvm("ktor-server-test-utils", ProjectRole.Library)
    val model: ConfiguredProject = multiplatform("model", ProjectRole.Library, Jvm, Js, WasmJs)
    val modelHealth: ConfiguredProject = multiplatform("model-health", ProjectRole.Library, Jvm, Js, WasmJs)
    val modelPaging: ConfiguredProject = multiplatform("model-paging", ProjectRole.Library, Jvm, Js, WasmJs)
    val openapiGenerator: ConfiguredProject = jvm("openapi-generator", ProjectRole.Library)
  }

  class Units(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":units"), ProjectType.Intermediate, ProjectRole.Container) {
    val unit: ConfiguredProject = multiplatform("unit", ProjectRole.Library, Jvm, Js, WasmJs)
    val unitConversion: ConfiguredProject = multiplatform("unit-conversion", ProjectRole.Library, Jvm, Js, WasmJs)
    val unitUtils: ConfiguredProject = jvm("unit-utils", ProjectRole.Library)
  }

  class WebResources(parent: ConfiguredProject) : ConfiguredProject(parent, GradleProjectPath(":web-resources"), ProjectType.Intermediate, ProjectRole.Container) {
    val webFonts: ConfiguredProject = other("web-fonts", ProjectRole.Library)
  }
}
