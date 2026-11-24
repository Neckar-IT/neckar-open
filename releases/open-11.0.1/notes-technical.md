### Added

- `submitKeycloakSignIn` in `playwright-config`, the Keycloak sign-in for an app whose first page has no `h1` ([!21364](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21364))
- `LockableButton` and `DisabledReasonTooltip` in `typescript-react-ui`; `ActionLockProvider` now also locks `FormSubmitButton`, `ConfirmButton`, `SaveDialog` and `CascadingDraftResetWarningDialog` ([!21331](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21331))
- `disabledReason` option of `useUpsertAction`, shown as the tooltip of the disabled save button of an archived record ([!21308](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21308))
- `hint` and `href` props of `StatusBadge`: a focusable tooltip and an optional link ([!21293](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21293))
- `useGetNow` hook and `nowMockedValue` in the `NowProvider` of `typescript-react-utils`; `UnknownOutcomeAlert` takes its reload time from `NowProvider` ([!21292](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21292))
- `NullableBooleanFormField` in `typescript-react-ui`: an optional boolean as yes, no or empty ([!21260](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21260))
- `@neckar-hub/script-money`: cent arithmetic with the semantics of `Money` for pricing scripts, with its globals declaration `./globals` and the case corpus `scriptMoney.cases.json` shared with the Kotlin money rules ([!21256](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21256))
- Value-timeline filters `validFromAfter` and `status` (`ValueTimelineEntryStatus` `Past`/`Current`/`Future` in `PriceTimelineEntriesFilterCriteria`) and the matching table filters of `ValueTimelineTable` ([!21254](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21254))
- `keycloakServerUrl` in `typescript-react-auth` (local Keycloak for `*.localdev` hosts), `localDevServerHost` in `vite-config` (`VITE_HOST`, rejects every-interface values) and `BindHost` in `ktor-server` ([!21219](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21219))
- `SidebarNavCategory`, a collapsible sidebar nav item with sub-items, in `typescript-react-ui` ([!21202](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21202))
- Test support exports `./draft-confirmed-test-support`, `./optimistic-locking-test-support` and `./data-display-test-support` of `typescript-react-ui` ([!21186](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21186))
- Test support from Commons: `userWithCapabilities` (`typescript-react-auth/test-support`), `QueryClientForTest` (`typescript-react-query`), `./navigation-test-support` (`typescript-react-ui`) and the shared Vitest jsdom setup of `vite-config` ([!21184](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21184))
- `useServerCheckedResolver` form resolver: Zod first, then a server check of one field, cached for 30 s per query key ([!21180](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21180))
- `payment-formats` in `typescript-utils`: `validateIbanShape`, `validateBicShape`, `validateCardNumberShape`, `validateMandateReferenceShape`, `CardNetworks` and `parseExpiryDate`, tested against the case corpora of the Kotlin value classes ([!21173](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21173))
- `bootstrapObservability` and `grafanaTraceObservabilityConfig` in `typescript-otel-tracing`, and the page frame `AppMain`, `AppMobileHeader` and `AppDesktopHeader` in `typescript-react-ui` ([!21170](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21170))
- `List.pageInMemory` in `rest/model-paging` to sort and page an in-memory list ([!21168](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21168))
- `UsedBy.activeCountOf(type)` in `rest/model` ([!21165](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21165))
- `ValidationResult.of(errors)` and the `ValidationResult.errors` extension in `rest/model` ([!21162](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21162))
- `exemptFromServerSimulation()` route marker in `ktor-server`; health, metrics, meta, OpenAPI and MongoDB meta routes carry it ([!21158](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21158))
- Detekt rule `GitLabApiValueDefault` in the `detekt` rule set ([!21153](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21153))
- `TenantPreview` (id and name of a tenant) and `Tenant.preview()` in `multi-tenancy` ([!21140](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21140))
- `actionForArchiveState` in `typescript-react-ui`: archive action for an active entity, unarchive action for an archived one ([!21119](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21119))
- `Parameters.getInstantOrFail` in `ktor-server`: an unreadable `Instant` parameter answers 400 instead of 500 ([!21114](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21114))

### Changed

- Every `GenerateMavenPom` task fails on a POM dependency without a version ([!21278](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21278))
- `PageSectionHeading` scales with the viewport: `h1` `text-2xl sm:text-3xl`, `h2` `text-xl sm:text-2xl` ([!21250](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21250))
- **Breaking** `LocalDev.Address` is `10.11.12.13`; a local Ktor service listens on `127.0.0.1` and the Dev-IP only, and names a missing Dev-IP on a failed bind ([!21219](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21219))
- `openapi-generator` names the generic variant in `x-type-ref`; the Orval example extractor writes one file per distinct route example value ([!21203](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21203))
- **Breaking** `formatUserName(user, fallback)` takes the fallback as second argument and accepts `null` names ([!21178](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21178))
- `ConfirmationDialog` reports a rejected `onConfirm` through `withActionFailedToast`; `EntityArchiveDialog` and `EntityUnarchiveDialog` no longer wrap it themselves ([!21175](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21175))
- **Breaking** `ObservabilityConfig` and `TraceIdPlaceholder` moved from `typescript-react-error-handling` to `@neckar-hub/typescript-utils/observability` ([!21170](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21170))
- `FeatureFlag.slowServer` and `FeatureFlag.forceServerError` apply to every route of a Ktor service, after authentication, instead of opt-in per route via `featureFlags().handle()` ([!21158](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21158))
- **Breaking** `UserDetails.tenantId` replaced by `tenant: TenantPreview?`; the JSON field is `tenant` with `id` and `name` ([!21140](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21140))
- **Breaking** `EntityArchiveDialog` takes `messages: EntityArchiveMessages` (`archived`, `refused`, `failed`) instead of `archivedMessage` and toasts every archive answer ([!21120](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21120))

### Removed

- **Breaking** `createEntityDialogAction` and `EntityDialogActionOptions` removed from `typescript-react-ui` ([!21378](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21378))
- **Breaking** `BadgeWithHint` removed; use `StatusBadge` with `hint` and `href` ([!21293](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21293))
- **Breaking** `ArchivedBadgeVariants` removed; `ArchivedBadge` and `EntityStatusBadge` take `StatusBadgeVariant` ([!21276](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21276))
- **Breaking** `TimelineEntryStatus` type removed from `typescript-react-ui`; `classifyTimelineEntry` returns `ValueTimelineEntryStatus` ([!21254](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21254))
- **Breaking** `X.Companion.Example` accessors and the empty companion objects removed from the value classes in `customer`, `contact`, `payment`, `kotlin-lang`, `kotlin-js` and `business-domain`; read the `XExample` object directly ([!21229](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21229))
- **Breaking** `DeploymentStage.Sandbox` removed ([!21219](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21219))
- **Breaking** `Span.attributes { }` removed from `ktor-server`; call `span.set(…)` directly ([!21145](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21145))
- **Breaking** `createEntityQueries`, `entityQueryKeys` and `useIdMutation` removed from `typescript-react-query` ([!21130](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21130))
- **Breaking** Ladle support removed: the `./ladle` export of `typescript-react-utils` and `ladleSuiteConfig` of `playwright-config` ([!20916](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/20916))

### Fixed

- Published POMs name the version of every MeisterCharts sibling dependency ([!21278](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21278))
- `Inflector.singularize` keeps words like `drives` as `drive`; only a fixed list of `-ves` words becomes `-f`/`-fe` ([!21268](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21268))
- `SpecHarvestProcessor` fails the compilation when `kotlinx.serialization.Serializable` does not resolve, instead of writing an empty harvest to the build cache ([!21251](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21251))
- The static-site policy check of `playwright-config` opens the first story of `index.json` instead of an autodocs page ([!21205](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/21205))
