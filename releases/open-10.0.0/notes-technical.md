### Breaking — Maven coordinates

Every Maven coordinate is now derived from the module's directory structure ([!15885](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15885)). 24 artifacts move to a new groupId and 2 artifacts are renamed. The artifacts under the old coordinates stop receiving updates at `9.0.1` — consumers must rewrite their dependency declarations.

New groupId, artifact name unchanged:

| Old coordinate | New coordinate |
|---|---|
| `it.neckar.open:boxing-verifier` | `it.neckar.open.ksp:boxing-verifier` |
| `it.neckar.open:ksp-commons` | `it.neckar.open.ksp:ksp-commons` |
| `it.neckar.open:ksp-test-utils` | `it.neckar.open.ksp:ksp-test-utils` |
| `it.neckar.open:serialization-verifier` | `it.neckar.open.ksp:serialization-verifier` |
| `it.neckar.open:spec-harvest` | `it.neckar.open.ksp:spec-harvest` |
| `it.neckar.open:spec-harvest-model` | `it.neckar.open.ksp:spec-harvest-model` |
| `it.neckar.open:ts-declaration-generator` | `it.neckar.open.ksp:ts-declaration-generator` |
| `it.neckar.open:types-list-generator` | `it.neckar.open.ksp:types-list-generator` |
| `it.neckar.open:mattermost-client-api` | `it.neckar.open.integrations:mattermost-client-api` |
| `it.neckar.open:youtrack-client` | `it.neckar.open.integrations:youtrack-client` |
| `it.neckar.open:meistercharts-additional-charts` | `it.neckar.open.meistercharts:meistercharts-additional-charts` |
| `it.neckar.open:meistercharts-canvas` | `it.neckar.open.meistercharts:meistercharts-canvas` |
| `it.neckar.open:meistercharts-config` | `it.neckar.open.meistercharts:meistercharts-config` |
| `it.neckar.open:meistercharts-core` | `it.neckar.open.meistercharts:meistercharts-core` |
| `it.neckar.open:meistercharts-data-api` | `it.neckar.open.meistercharts:meistercharts-data-api` |
| `it.neckar.open:meistercharts-data-client` | `it.neckar.open.meistercharts:meistercharts-data-client` |
| `it.neckar.open:meistercharts-data-server` | `it.neckar.open.meistercharts:meistercharts-data-server` |
| `it.neckar.open:meistercharts-easy-api` | `it.neckar.open.meistercharts:meistercharts-easy-api` |
| `it.neckar.open:meistercharts-examples` | `it.neckar.open.meistercharts:meistercharts-examples` |
| `it.neckar.open:meistercharts-fx` | `it.neckar.open.meistercharts:meistercharts-fx` |
| `it.neckar.open:meistercharts-history-api` | `it.neckar.open.meistercharts:meistercharts-history-api` |
| `it.neckar.open:meistercharts-history-core` | `it.neckar.open.meistercharts:meistercharts-history-core` |
| `it.neckar.open:meistercharts-react-kotlin` | `it.neckar.open.meistercharts:meistercharts-react-kotlin` |
| `it.neckar.open:meistercharts-version-info` | `it.neckar.open.meistercharts:meistercharts-version-info` |

Renamed artifacts (groupId `it.neckar.open` unchanged):

| Old coordinate | New coordinate |
|---|---|
| `it.neckar.open:conversion` | `it.neckar.open:unit-conversion` |
| `it.neckar.open:utils` | `it.neckar.open:unit-utils` |

`it.neckar.open:unit` and every artifact under `internal/open/commons/` keep their coordinate.

### Added

- Value classes with derived properties are supported by the KSP TypeScript declaration generator ([!15882](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15882))
- `EntityMultiSelectFormField` in `typescript-react-ui` for shared multi-select form fields ([!15863](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15863))
- Static nginx sites can be declared as `DeploymentDescriptor` with local `https://<name>.localdev` routes ([!15699](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15699))
- `InlineValueObjectListFormSection` for embedded value-object lists ([!15666](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15666), [!15589](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15589))
- `@Hot` annotation plus Detekt rules against value-class boxing in hot paths ([!15641](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15641)), extended to deny `@Slow`/`@Allocates` calls and property reads inside `@Hot` bodies ([!15662](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15662))
- Pluggable theming with the terminal, aurora, paper and fjord themes plus `ThemeProvider`/`ThemeSwitcher` ([!15615](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15615))
- `${image:<repository>}` placeholder as the single source for deployment image references, validated by `verifyDeploymentImages` ([!15614](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15614))
- Endpoint-driven OpenTelemetry enablement, superseding ADR 0155 ([!15583](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15583))
- JFR-based per-paint allocation recording for MeisterCharts ([!15550](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15550))
- `wasmJs` target added to the `meistercharts-canvas` `commonMain` tree ([!15549](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15549))
- Embedded catalog-entity previews in battery, assembly and electricity-work configuration REST views ([!15533](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15533))
- OpenTelemetry agent configuration via a convention plugin as the single default source, with every default overridable through environment variables ([!15519](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15519))
- Custom assertk assertions for pagination, `isNotNullAnd`, `hasSingleElement` and `messageContainsAllOf` ([!15494](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15494))
- `@oidc-middleware` directive that deduplicates the traefik-oidc-auth labels and gives all services persistent sessions ([!15470](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15470))
- Continuous deploy rolls out affected hosts and services on every `main` merge ([!15457](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15457))
- OpenAPI schema features: value-class constraints, `readOnly`/`writeOnly`, `deprecated` and a default response ([!15438](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15438)), plus the `x-deprecated-reason` and `x-enum-descriptions` extensions ([!15444](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15444))
- Spec-driven code-generation platform: fachliche specs feed a deterministic full-stack generator ([!15429](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15429))
- OpenTelemetry resource attributes derived from `RuntimeContext` through an agent extension ([!15416](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15416))
- `toggleGroupFromEnumFormField` — an enum-driven `ToggleGroup` form field analogous to `SelectFromEnumFormField` ([!15369](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15369))
- `ValidationResult` in the shared generated types plus a `needsEditing` utility ([!15334](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15334))
- `FilterDescriptor.localDateValue()` and an end-to-end `LocalDate` date-range filter ([!15330](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15330))
- `AnalyticsProvider` is router-independent ([!15145](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15145))
- `internal/CATALOG.yaml` as the central domain type catalog ([!15143](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15143))
- Typesafe `Logger` creation helper ([!15142](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15142))
- Keycloak silent check-sso avoids the double application boot ([!15053](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15053))
- `KtorServerTelemetry` reports 4xx span status and MDC allow-list attributes ([!15051](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15051))
- Shared health layer with live/ready/health probes, state holder, readiness gating and dependency probes ([!15022](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15022)), consolidated into one health system with uncaught exceptions reported via OpenTelemetry ([!15521](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15521))
- Global error boundary shows request details and a Grafana trace link on every error page ([!15000](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15000))
- Typesafe i18n keys ([!14971](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14971))
- Complete HTTP status assertion family in `RestAssertions` ([!14690](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14690))
- Self-describing password hashing with a sealed `PasswordHashScheme` ([!14603](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14603))
- Generic price-timeline components extracted into `typescript-react-ui` ([!14581](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14581)); `PriceSummary` moved into the price-timeline bundle with an "Add now" empty-state link ([!14734](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14734))
- Component catalog for `typescript-react-ui` ([!14580](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14580)) with meaningful JSDoc descriptions per component ([!14678](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14678))
- `descriptors` and `docker-model` published as open modules ([!14494](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14494)); `AnsiConsole` and the Docker model types extracted into build-logic modules ([!13676](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13676))
- `provisionedUser` call-context helper with a per-call cache ([!14389](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14389))
- DNS zone roundtrip — pull, diff and apply for INWX and Hetzner DNS ([!14387](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14387))
- Detekt rule `RespondRequiresExplicitTypeArgument` ([!14386](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14386)), generalized to `PolymorphicSerializationCallRequiresExplicitType` ([!14390](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14390))
- Data-migration module plus the `dependencyFence` Gradle plugin ([!14332](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14332))
- `FormattedDate` atoms and `dateColumn` helpers ([!14256](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14256))
- `useAutoPageTitle` hook that derives `document.title` automatically ([!14068](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14068))
- Datetime-range filter pattern ([!14066](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14066))
- `TruncateTextContent` supports nested `data-text-content` through a link wrapper ([!14063](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14063)), with `installTruncateTextContentOnBody` ([!14062](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14062)) and global truncation support ([!13659](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13659))
- `JsonElement`/`JsonArray` assertk extensions for JSON navigation in tests ([!14035](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14035))
- Selection-with-Aggregates pattern ([!13997](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13997))
- Entity Property Renderer with factory, `*Link` component and a four-state contract ([!13951](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13951))
- Detekt findings ([!13912](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13912)) and oxlint reports ([!14020](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14020)) feed the GitLab Code Quality widget; Detekt reports published via reports.neckar.it ([!13797](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13797))
- Skeleton table renderer for the `DataSliceView` pending mode ([!13879](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13879))
- Detekt rule for `require()`/`check()` without a message lambda ([!13871](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13871))
- `useKeycloakLogout` hook in `typescript-react-auth` ([!13866](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13866))
- `commons/workflow` module with `TransitionInfo` ([!13781](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13781))
- `service_host` OpenTelemetry label end-to-end across backend, frontend, collector and dashboard ([!13711](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13711))
- Ladle stories for `typescript-react-ui` hosted at react-ui.showcase.neckar.it ([!13681](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13681))
- `EntityStatusBadge` replaces the separate `ConfirmationStatusBadge` ([!13667](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13667))
- Example objects for REST DTOs, domain entities and fixtures ([!13609](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13609))
- Detekt rule `MissingClientSessionRule` ([!13559](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13559)); RestResponse factory enforcement and respond-with-sealed-subclass rules ([!13963](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13963))
- `UnstyledButton` primitive; raw `<button>` call sites migrated ([!13554](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13554))
- `LanguageSwitcher`, `Breadcrumbs` and `EntitySelectFormField` extracted into commons ([!13431](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13431)); further reusable components extracted into the shared library ([!13346](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13346))
- Special i18n languages Dev (shows keys) and Large (layout stress) ([!13414](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13414)), applied on the fly in the `t` wrapper ([!13673](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13673))
- Star-rating input component ([!13347](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13347))
- `vite-config` plugin for commons hot module replacement in dev mode ([!13323](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13323)), enabled through the `commonsSourceExports` plugin in the remaining Vite/Vitest consumers ([!13704](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13704))
- `ResponsiveTable` renders a card view on small screens instead of breakpoint-based column definitions ([!13286](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13286)) and supports a render function for `ColumnDefinition.header` ([!13167](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13167))
- Differentiated auth error states with auto-recovery ([!13252](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13252))
- OpenTelemetry traces carry device type, browser route, service version and feature flags ([!12969](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/12969))

### Changed

- Documentation root standardised on `docs/` (plural) across the repository ([!15907](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15907))
- **Breaking** Commons TypeScript packages grouped under `commons/typescript/` ([!15864](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15864)); `internal/open` root structure cleaned up ([!15834](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15834))
- `suggestedRoofInsets` is a required, pre-filled and editable field in the roof configuration form ([!15819](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15819))
- Embedded value-object lists are edited inline via `FieldArray`; the table-plus-dialog variant was dropped ([!15754](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15754))
- Navigation is gated on the target page instead of at entry, making it independent of the edit state ([!15721](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15721))
- Request tracking on docker-hosting gives end-to-end traceability of all HTTP requests ([!15718](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15718))
- Shared `rotatingSubDomain()` helper extracted in `SlippyMapProvider` ([!15506](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15506))
- Auth-provider names derived from the `ServiceAuthentication` configuration ([!15469](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15469))
- MeisterCharts: allocation-free paint and layout hot paths ([!15460](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15460)), `Path` stores actions in flat opcode/coordinate buffers instead of `PathAction` objects ([!15463](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15463)), stripe-painter value-class boxing eliminated in the timeline hot loop ([!15546](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15546)), trim policy for grow-only layout buffers ([!15510](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15510)), exact collision-free label placement in `LabelPainter2` ([!15479](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15479))
- MeisterCharts: unified demo control API (`configurable`/`configurableSeparate`) with a consistent `section()` ([!15453](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15453)); config-object pattern consolidated ([!15378](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15378), [!15428](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15428), [!15476](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15476))
- Reproducible builds: deterministic Gradle archives, base-image digest pinning and bake timestamps ([!15427](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15427)); git hash injected at the artifact edges instead of as a compile input ([!15612](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15612)); `gitHashShort` and `git describe` made machine-independent ([!15431](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15431)); volatile git info moved into the private leaf module `version-info-volatile` ([!15590](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15590)); redundant `buildDate` removed from `version-info` ([!15414](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15414))
- OpenTelemetry records exceptions on the span; the orchestrator parse-error special case was dropped ([!15382](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15382))
- Meta, debug and OpenAPI-doc endpoints are secured deny-by-default ([!15346](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15346))
- pnpm catalog introduced (default catalog, `catalogMode: strict`) and the workspace `package.json` files migrated ([!15324](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15324))
- Loki labels: the legacy `app`/`host` labels replaced by `service_name`/`service_instance_id` ([!15302](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15302))
- Ktor service wiring consolidated onto the `installNeckarServiceDefaults` helper ([!15263](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15263))
- `ResponsiveTable` with few columns uses the full available width ([!15260](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15260))
- pnpm `package.json` scripts unified with `build:noDeps` as the single per-package unit ([!15062](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15062)); `typescript@6.0.3` declared as a devDependency in all TypeScript packages ([!14527](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14527))
- **Breaking** `startStartOfHour` renamed to `startOfHour` ([!14972](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14972))
- CORS consolidated onto the descriptor-driven helper ([!14927](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14927)), with credentials kept off for `anyHost` services ([!14942](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14942))
- **Breaking** `tenantId()` renamed to `jwtTenantId()` and `currentTenantId()` to `userTenantId()` ([!14840](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14840)); `tenantId()` fails fast instead of falling back to a synthetic default ([!14832](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14832))
- `Uuid.pseudoRandom()` produces RFC-4122 v4 identifiers ([!14803](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14803))
- **Breaking** Platform headers renamed to the `NeckarIT-*` prefix per RFC 6648 ([!14791](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14791)); tracking headers folded into `PlatformHeader` ([!14948](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14948))
- Kotlin upgraded to 2.4.0, including the JS value-class export and ES2015 inlining evaluation ([!14662](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14662)); context parameters adopted ([!15399](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15399))
- **Breaking** `Money` converted to a `@JvmInline` value class ([!14559](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14559))
- `DescriptionList` and `DetailsList` consolidated ([!14554](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14554)); layout components consolidated across the UI library ([!13962](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13962))
- `Translate` split into an app-level `Translate` (i18n) and `TranslateEmbedded` (ui) ([!14541](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14541)); `TextKey` passed as a property instead of as children ([!14532](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14532))
- `toast.error` is visually more prominent ([!14528](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14528))
- tsconfig reworked: `dom.json` introduced, the base neutralized and `lib` raised to ES2023 ([!14459](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14459))
- `MemberRole` enum replaced by a Capability/RoleLabel model ([!14417](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14417))
- **Breaking** `DataSliceOptions` renamed to `DataSliceController` ([!14328](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14328))
- Frontend null handling: upsert forms use nullable zod schemas with null defaults ([!14299](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14299), [!13313](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13313)), following the react-ui null/undefined convention ([!14143](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14143))
- Domain and REST DTOs separated by introducing `DataViews` on the response side ([!14128](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14128))
- `ConfirmationStatusBadge` uses semantic color tokens ([!14054](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14054))
- Highlight text is passed via React context instead of prop drilling ([!14018](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14018))
- Java service logs unified via stdout JSON and the OpenTelemetry agent pipeline ([!14016](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14016), [!14015](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14015), [!14021](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14021))
- `TrimmedString` is restricted to request DTOs — domain, entity and view layers use plain `String` ([!13979](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13979))
- **Breaking** `optionRenderer` renamed to `itemRenderer` ([!13957](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13957), [!13960](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13960))
- `openapi-merge-cli` replaced by `@redocly/cli` for native OpenAPI 3.1 support ([!13949](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13949)); the OpenAPI spec output is declared only on the `:openapiSpec` task ([!13769](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13769))
- **Breaking** `typescript-shared-types` merged into `typescript-utils` ([!13843](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13843))
- Detekt: 16 bug-finding rules enabled ([!13806](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13806)), 8 further rules activated with generated baselines ([!13612](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13612)), baselines pruned and layer-separation plus native-type rules added ([!14166](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14166)), 5 idiom-style rules demoted to non-blocking info severity ([!13930](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13930))
- `ArchiveScopeSwitch` replaced by `FilterByArchived`; the backend default renamed to `OnlyActive` ([!13719](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13719)); `FilterArchived` aligned with the `FilterByItems` design ([!14331](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14331))
- Title components reduced to pure text; `EntityStatusBadge` moved to the link layer ([!13706](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13706))
- Commons exports simplified via native Node subpath patterns ([!13675](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13675)); `typescript-utils` exports refactored to a single wildcard entry ([!13779](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13779))
- `@vitejs/plugin-react-swc` replaced by `@vitejs/plugin-react` ([!13666](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13666))
- `ArchivedEntityBanner` migrated to the shared `Alert` primitive ([!13642](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13642)) and moved into the shared UI component library ([!14556](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14556))
- **Breaking** REST response discriminator values `success`/`failure` renamed to `good`/`bad` ([!13451](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13451)); the nested response subtypes `Success`/`Failure` renamed to `Good`/`Bad` ([!13456](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13456)); `RestAssertions.isSuccess`/`isFailure` renamed to `isGood`/`isBad` ([!13594](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13594))
- `QueriesResolved` built on `QueriesRaw` with aligned `Query*` naming ([!13450](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13450)); `QueriesData`/`QueriesDataExtended` state handling completed ([!13427](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13427)); overview pages migrated to `QueriesRaw` with a pending state ([!13601](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13601)); `QueryResult` slimmed down and decoupled from `UseQueryResult` ([!13487](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13487)); the query state resets only on actual success, not on `bad/*` responses ([!13561](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13561))
- Kotlin/JS npm versions decoupled from `gradle/npm.versions.toml` ([!13446](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13446)); `package.json` is generated directly and checked in ([!13870](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13870), [!13897](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13897))
- Gradle upgraded from 9.3.1 to 9.4.1 ([!13432](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13432)); `buildSrc` migrated to `build-logic` ([!14297](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14297)) and split into core, projects and plugins ([!14357](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14357))
- Kotlin exception handling around JSON parsing simplified with kotlinx.serialization 1.11.0 ([!13412](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13412))
- `ConfirmationDialog` takes title and description as text keys ([!13333](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13333))
- 683 AsciiDoc files migrated to Markdown ([!13066](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13066), [!13067](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13067))
- ESLint replaced by Oxlint ([!12807](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/12807)); the Oxlint warning policy is enforced by promoting rules to error or disabling them explicitly ([!14673](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14673)); switch statements require a throwing `default` case ([!14089](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14089))

### Deprecated

- `DataTable` marked as deprecated ([!13478](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13478)), with the retention rationale documented in its JSDoc ([!14558](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14558))

### Removed

- watchtower removed in favour of continuous deploy ([!15457](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15457))
- `removeNulls` walkers removed ([!14324](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14324)); `nullToUndefined` and `undefinedToNull` removed ([!14299](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14299))
- Dead `npm.versions.toml` infrastructure removed ([!14093](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14093))
- `auth-rest` module dissolved into the common REST sub-package layout ([!14073](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14073))
- Deprecated `QueryComponent` and `SuspenseQueryComponent` removed ([!13523](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13523))
- `ResponsiveDataTable` removed ([!13478](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13478))
- FontAwesome and SCSS removed from the theme generator ([!13423](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13423))
- Dead exports removed from `typescript-react-ui` ([!13321](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13321))

### Fixed

- Nullable numeric floating inputs no longer render the literal `null` ([!15950](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15950))
- `ConverterListCell` and `EnumListCell` reset their text on empty or null cells ([!15861](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15861))
- TypeScript generation preserves the outer nullability of `List` and `Array` types ([!15858](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15858))
- `isFailToFetch` matches the actual browser message "Failed to fetch" ([!15855](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15855))
- Swapped Back/Forward mouse-button mapping (DOM buttons 3 and 4) ([!15839](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15839))
- Random generators: CMWC4096 carry truncation and unsigned state handling ([!15838](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15838)); MWC256 byte-index mask crash after 128 draws ([!15769](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15769))
- `UndoState` caps the previous history after appending on minor changes ([!15830](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15830))
- `findClientId` returns a `ClientId` instead of a `ClientRole` ([!15827](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15827))
- KSP generator: qualified-name builtin matching, transitive sealed check and bounds checks ([!15825](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15825))
- `Runtimes.destroyProcess` uses the pre-destroy process info in its error message ([!15813](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15813))
- Vite build out-of-memory caused by Tailwind `@source` scanning the package root ([!15812](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15812))
- `X509Support` cipher/decipher confidentiality direction ([!15780](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15780))
- `DateUtils.formatDurationWords` localizes the zero-duration string ([!15772](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15772))
- `GlobalErrorState` guards `onErrorCallbacks` against concurrent modification ([!15770](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15770))
- Mattermost `PostsResponseParser` returns the raw message text and sorts by order ([!15768](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15768))
- `PageSize(0)` is rejected — it caused a division by zero in `PaginationInfo` ([!15767](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15767))
- `PendingRequestsState.calculateProgress` divides by the total instead of by the pending count ([!15765](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15765))
- `JacksonMillisAsIsoDateTimeDeserializer` accepts integral numbers ([!15763](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15763))
- Kotlin `Float` maps to OpenAPI `float` instead of `double` ([!15762](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15762))
- Wrong SI prefix factors for mega, giga, tera, pico and zepto ([!15761](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15761))
- `VersionRange.containsCompletely` honors the other range's border inclusivity ([!15757](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15757))
- `Resolutions._800_480` pointed at XGA instead of WVGA ([!15755](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15755))
- `CollectingCoroutineExceptionHandler.getExceptions` returns a snapshot ([!15705](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15705))
- `HashAlgorithm.calculate` accepts valid alternative algorithm names ([!15704](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15704))
- `List.shifted` crash on negative shift counts ([!15703](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15703))
- `RightTriangleType.format` quadrant labels ([!15694](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15694))
- Overflow guard in `MoneyFraction.times(Int)` ([!15693](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15693))
- `LoggerName.shortened` crash on empty name segments ([!15692](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15692))
- `Inflector.ordinalize` suffix for teens inside larger numbers ([!15691](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15691))
- Forms: a Cancel click survives blur validation, and dialog forms use the page-form field spacing ([!15683](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15683))
- Cookie parsing splits `key=value` on the first `=` only ([!15675](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15675))
- Double-cache guard type in `CachedDateTimeFormat` and `CachedLocalDateFormat` ([!15674](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15674))
- `IntIntMap.equals`, `CacheMap.updateMaxSize` and `Array2` margin stripping ([!15673](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15673))
- `KClassSerializer` round-trip for primitive `KClass` values ([!15665](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15665))
- `LocalTime.format` millis padding and `LocalDate.plusMonths` across year boundaries ([!15664](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15664))
- `DisposeSupport.dispose` and `Disposable.all` are exception-safe ([!15586](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15586))
- Getter branch of `KProperty.findAnnotationsSuper` ([!15585](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15585))
- `FastByteArrayInputStream.skipToAlign` and string decoding ([!15584](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15584))
- nginx readiness retry loop re-issued the HTTP request ([!15572](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15572))
- XDG empty-environment fallback and the Windows PDF launcher exec ([!15571](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15571))
- `DoublesProvider.max()` initial value for negative-only values ([!15570](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15570))
- String interpolation of the heartbeat check description in logging ([!15537](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15537))
- `HttpStatusCode` descriptor kind (INT) and readiness path-segment matching ([!15536](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15536))
- `FileCopyManager` delegates to the stdlib file operations, fixing silent truncation and a file-descriptor leak ([!15535](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15535))
- `CardNumber` masking for non-16-digit card numbers (Amex, Diners) ([!15534](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15534))
- `deliveryDate` migration resolves the Berlin day independently of the system timezone ([!15507](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15507))
- Automatic logout despite user activity — refresh-token replay on concurrent refreshes ([!15459](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15459))
- 31 TypeScript `no-deprecated` findings resolved ([!15211](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15211))
- `ClientUpdatesSupport` maps guarded against concurrent access ([!15185](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15185))
- Table column header/body alignment and content overflow ([!15044](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15044))
- Assorted small correctness fixes across commons and MeisterCharts: `ScreenshotCreatedWithWarningReportCreator` require message ([!15008](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15008)), `ErrorCode` split limit, `VersionRange` typo and DPI loop variable ([!15005](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15005)), `vmProperty` parameter typo in `JavaCommandBuilder` ([!15003](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15003)), copy-paste in require messages ([!14973](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14973)), `PriorityQueue.hashCode` comparator ([!14970](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14970)), `MutableSortedList` `ListIterator.set` precondition ([!14969](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14969)), `BaseTimerImplementation` message variable and `IntRange.toIntList` capacity ([!14968](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14968)), `RelativeMulti` parameter name append ([!14965](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14965)), `KEY_SERVICE_HOST` hostname resolution ([!14964](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14964)), font-metrics off-by-one and `minDeviation` check ([!14960](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14960))
- Systematic MeisterCharts bug sweep — coordinate and zoom conversions ([!14909](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14909), [!14910](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14910), [!14931](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14931), [!14932](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14932), [!14935](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14935), [!14934](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14934)), sigmoid gradient formulas ([!14936](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14936), [!14937](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14937), [!14938](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14938), [!14901](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14901)), down-sampling boundaries ([!14939](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14939), [!14863](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14863)), painter and layer defects ([!14908](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14908), [!14902](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14902), [!14906](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14906), [!14907](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14907), [!14833](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14833), [!14900](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14900), [!14883](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14883), [!14884](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14884)), history and reference-entry defects ([!14941](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14941), [!14897](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14897), [!14904](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14904)), tile-index overflow ([!14911](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14911)), observable listener snapshots ([!14894](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14894), [!14873](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14873))
- Security hardening: `XmlCommons` guarded against XXE and external-entity attacks ([!14905](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14905)); constant-time equality for hashes and signatures with a warning on MD5/SHA-1 ([!14899](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14899)); `FileHistoryStorage` delete result checked ([!14904](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14904))
- Spurious "Ungültige UUID" on entity-id form references ([!14799](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14799))
- Systematic commons bug sweep — `ExceptionPurger` infinite loop, Zip Slip, `Money` division, CSV/Timer/HashAlgorithm defects ([!14777](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14777)), `Tags` stack overflow, `nullError`, search params, i18n leak, Promise and LocalStorage iteration ([!14808](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14808)), uuid, time and TypeScript-generator defects ([!14762](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14762)), `Either` messages, `randomNormal` log base and `toRelativeValues` guard ([!14716](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14716)), negative arguments and the Feb 29 edge in `plusDays`/`plusYears`/`plusMinutes` ([!14823](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14823)), `equals`/`hashCode` contracts in `CacheMap`/`PriorityQueue`/`Pool` ([!14811](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14811)), off-by-one errors in `readStringz`, `Retry`, `sliceUint8` and `PredefinedConfiguration.equals` ([!14859](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14859)), `PythonRunner` equals/hashCode, `Hex` StringBuilder and `ClientUpdatesSupport` magic number ([!14865](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14865)), `Distance.times`/`multiply` Y bug and `MinMaxValues` aggregation ([!14819](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14819)), javafx, kotlin-js and exception-handling-ui defects ([!14826](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14826)), `ZipCreator` directory entries ([!14912](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14912)), `writeBytesWithRename` ([!14898](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14898)), `FileExt.renameTo` ([!14879](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14879))
- `ValidationMessages` tooltip in the `HoverCard` rendered at the left window edge instead of below the component ([!14774](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14774))
- `MultiStepControl` instance survives provider re-renders ([!14495](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14495))
- `execStreamed` uses `ExecOperations` instead of `ProviderFactory.exec` ([!14413](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14413))
- NPE risks (`UnsafeCallOnNullableType`, `HasPlatformType`) fixed in commons ([!14249](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14249)) and MeisterCharts ([!14253](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14253)); exception throws in `kotlin-collections` given messages ([!14250](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14250)); coroutine cancellation honored in catch blocks ([!14245](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14245))
- Oxlint error-class findings (JSX context, unused variables, promise return) ([!14216](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14216)); unused `eslint-disable` directives removed ([!14492](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14492))
- Duplicate horizontal separator in the `DateTimePicker` ([!14156](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14156))
- Breadcrumb truncation shows an ellipsis ([!14146](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14146))
- `DateRangePicker` hides the clear button when the value is `null` ([!14117](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14117)); the undefined API was restructured ([!14121](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14121))
- OpenAPI generator resolves transitive interface inheritance for `@OpenApiBaseSchema` ([!14022](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14022)); nullable-plus-default bug fixed ([!14324](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14324)); OpenAPI type drift in `gitlab-auto-merge-ui` ([!14211](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14211))
- Explicit type annotations for platform types in `BufferJvm.kt` ([!13992](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13992))
- Text overflow ([!13968](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13968)) and italic text clipped at the right edge when truncated via `overflow: hidden` ([!13699](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13699))
- `SelectFormField` chevron color and trigger width without a selected value ([!13919](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13919))
- `KeyboardNavigation` listener crash on Chrome autofill events ([!13859](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13859))
- `println` calls replaced by proper APIs across modules ([!13823](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13823)); bare `require()` calls given custom messages ([!13835](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13835))
- macOS build failure in `fix-openapi-version.sh` ([!13671](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13671)); `Toast.tsx` renamed to `ToastProvider.tsx` to fix the macOS case collision ([!15389](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15389))
- `Alert` icon alignment for the description-only layout ([!13646](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13646))
- Detekt baseline cleanup: `UseOrEmpty` ([!13633](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13633)), `ImplicitDefaultLocale` ([!13630](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13630)), `EmptyFunctionBlock`/`UnusedPrivateFunction` ([!13629](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13629)), `UnsafeCallOnNullableType` ([!13626](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13626)), `IgnoredReturnValue` ([!13965](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13965)), `UnreachableCode` ([!13964](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13964)), `DoubleMutabilityForCollection` ([!14155](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/14155)), `VarCouldBeVal` ([!13927](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13927)), plus assorted single-instance findings ([!13619](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13619), [!13620](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13620), [!13625](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13625))
- Distinct UI states for the Keycloak-init and `/me`-request phases ([!13618](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13618)); Keycloak race condition fixed by waiting for the token instead of returning `null` ([!12989](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/12989))
- `NumberInput` float drift after the `conversionFactor` division ([!13584](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13584)); centering without +/- buttons and the layout shift when removing the clear button ([!13209](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13209))
- Global error handler logs caught errors to the devtools console ([!13582](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13582)) and logs before automatic and manual reloads ([!13583](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13583))
- Badges no longer hyphenate their text ([!13549](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13549))
- `File.delete()` result checked in `FileOutputStreamWithMove` ([!13501](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13501))
- `JSON.parse` in the error handler guarded against a non-JSON blob ([!13497](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13497))
- Subscription leaks in `Observable` `map`/`bind`/`select` ([!13492](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13492), [!13493](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13493))
- `RETURN_VALUE_NOT_USED` cleanup across kotlin-lang, kotlin-collections, rest, app, financial and concurrent modules ([!13477](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13477), [!13480](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13480), [!13482](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13482), [!13483](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13483), [!13488](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13488), [!13515](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13515), [!13527](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13527), [!13536](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13536), [!13595](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13595))
- Truncated test in `InlineCollectingTest` ([!13160](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/13160))
