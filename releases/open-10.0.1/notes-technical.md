### Added

- `typescript-react-ui` entry point `./value-timeline` with the value-neutral timeline building blocks `ValueTimelineTable`, `TimelineSummaryStacked`, `TimelineSummaryInline`, `TimelineSummaryStats`, `TimelineEntryStatusBadge`, `TimelineEntryTimingFormFields`, `TimelineEntryValidFrom`, `ValidFromDate`, `classifyTimelineEntry`, `useTimelineDataSliceControllerFromUrl`, `timelineEntryTimingSchema` and the `validFromColumn` / `statusColumn` / `rowActionsColumn` table columns ([!16175](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16175))
- `typescript-react-ui` entry point `./conditions-timeline` with `ArticleConditionsTimelineTable`, `ArticleConditionsTimelineEntryUpsertPurePage`, `ArticleConditionsSummary`, `ArticleConditionsSummaryHeadline`, `ArticleConditionsLine`, `ArticleConditionsAccessoryPrice`, `ArticleConditionsInstallationTime`, `PricePerUnitPurchasePrice`, `PricePerUnitSellingPrice`, `AddConditionsLabel`, `toArticleConditions` and the `articleConditionsColumns` table columns ([!16175](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16175))
- `CheckboxDependentFields` in the `typescript-react-ui` `checkbox-form-field` module ([!16175](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16175))
- `typescript-utils` modules `conditions-timeline` (`extractCurrentConditions`, `hasConditionsData`, `HasCurrentConditions`, `HasConditionsTimelineSummary`) and `duration` (`parseIsoDurationHours`, `formatIsoDurationFromHours`) ([!16175](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16175))
- `PricePerUnitWithProfit.purchasePrice` and `PricePerUnitWithProfit.sellingPrice` in `commons/money` ([!16174](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16174))
- `PricePerUnitWithProfit` and `PriceUnit` in `commons/money` ([!16172](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16172))
- `AdjustableValueSelect` and `AdjustableValueSelectFormField` in `typescript-react-ui` ([!16065](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16065))
- `useConnectCompanion`, `useConnectFormCompanion` and `recalculatedAdjustableValueText` for support-layer calculation of adjustable values ([!16036](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16036))
- Quantity components `eurosPerKilometer` and `hours` plus shared conversion factors in `typescript-react-ui` ([!16022](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16022))
- `gitHash` and `gitHashShort` meta tags in the Ladle showcase HTML head, resolved at serve time from the generated `.env` ([!16032](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16032), [!16072](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16072))
- `AdjustableValueDisplay` as the read-only rendering of an adjustable value ([!15995](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15995))
- `AdjustableValue.Manual` variant plus `AdjustableValueNumberInput`, `AdjustableValueTextInput` and `AdjustableValueAddressFormFields` ([!15961](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15961))
- `RestResponse` discriminator identity rule (`"good"` / `"bad/<Subtype>"`) enforced at build time by the OpenAPI schema generator, with `isBadResponseType` and `unexpectedRestResponseTypeError` in `typescript-utils` ([!15954](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15954))
- `LanguageCode` value class (ISO 639-1) and the `Locale.languageCode` extension in `commons/i18n` ([!15821](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15821))

### Changed

- `typescript-react-ui` entry point `./price-timeline` re-exports `classifyPriceTimelineEntry`, `PriceTimelineEntryStatus`, `PriceTimelineEntryStatusBadge`, `PriceTimelineDataSliceController` and `usePriceTimelineDataSliceControllerFromUrl` under their established names; the implementations now live in `./value-timeline` ([!16175](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16175))
- `internal/open/LICENSE` names Neckar IT GmbH as copyright holder and spells out the GPLv3 linking exception, matching the source-header notice ([!16221](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16221))
- German `ConfirmationStatus` label "in Bearbeitung" renamed to "Entwurf" ([!16179](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16179))
- Floorplan webapp: the saved-uploads link was dropped, the API follows the patterns conventions and exposes deploy metadata ([!16120](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16120))
- Published `internal/open` modules carry real POM descriptions ([!16073](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16073))
- `GlobalErrorHandler.versionInfo` is optional and defaults to `appGitInfo()`, which reads the git metadata from the served HTML instead of a compile-time constant ([!16055](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16055))
- `date` and `date-time` picker form fields are compile-checked against the form field value type ([!16051](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16051))
- **Breaking** `RuntimeContextEnv` and `RuntimeContextSys` moved from `it.neckar.runtime.detection` to `it.neckar.runtime.context`; `data/StatistikGeburtenDE.csv` is no longer shipped in the `csv` and `resource-loading` artifacts ([!16035](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16035))
- tsconfig unified across the TypeScript packages and the TS 6.0 deprecations resolved; `extractParamNames` and `parseLocalDate` fail fast on a missing path parameter or a malformed date ([!16003](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16003))
- KSP processors log their progress at `logging` instead of `info` level, keeping the build output quiet ([!15977](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15977))
- OpenTelemetry JavaScript SDK updated; `BatchLogRecordProcessor` takes its exporter as a config property ([!15944](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15944))

### Removed

- `requireGood` removed from `typescript-react-ui` ([!15954](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15954))

### Fixed

- `Button` with `asChild` crashed the render with "Slot failed to slot onto its children" — the loading spinner made it a two-child render, the content is now wrapped in `Slot.Slottable` ([!16258](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16258))
- `Sequence` chains of three or more operations dropped all but the last two ([!16092](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16092))
- Sidebar active-state matching for sibling routes that share a string prefix ([!16091](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16091))
- `ZodErrorTranslationsProvider` i18n key paths did not match `zod-errors.json` ([!16090](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16090))
- The logger localStorage scan treated the `rootLevel` key as a logger ([!16088](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16088))
- The repeat timer skipped its re-sort on an early return and dropped due callbacks ([!16086](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16086))
- `PaginationInfo.create` produced a negative `pageCount` for an unlimited page size ([!16085](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16085))
- `IntMap.equals` and `IntIntMap.equals` were missing the size check, causing asymmetric equality ([!16084](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16084))
- `Double.roundHalfUp` truncated negative non-tie values toward zero ([!16083](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16083))
- `TimePicker` displayed aligned times one step too high ([!16082](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16082))
- YouTrack custom-field deserialization crashed on an explicit `null` value ([!16080](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16080))
- `parseNumber` rounding for negative values and the `filterArrayBySearchTerm` crash on null entries ([!16077](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16077))
- `Int.isOdd` returned false for negative odd numbers ([!16076](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16076))
- `MoneyFraction.cents(Int)` integer overflow for amounts above ~214k EUR ([!16058](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16058))
- `TickInterval.MILLISECOND` dropped the milliseconds from the tick label ([!16052](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16052))
- `Resolutions` alias `_1140_900` corrected to `_1440_900` ([!15969](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15969))
- `File.isSymLinkTo` checks the actual link target instead of ignoring it ([!15968](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15968))
- `ZipExtractor` creates the missing parent directories for nested file entries ([!15967](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/15967))

### Security

- The floorplan uploads archive is guarded by a URL token ([!16123](https://git.neckar.it/neckarit/neckar-hub/-/merge_requests/16123))
