package it.neckar.projects.sharing

/** The [SharedFileKind] entries the projects under `internal/open` publish; the export of `internal/open` resolves them without the monorepo. */
enum class OpenSharedFiles : SharedFileKind {
  /** The `*.cases.json` conformance corpora a payment value class keeps in `src/jvmTest/resources`; the Vitest of `payment-formats` in typescript-utils reads them. */
  PaymentFormatCases,

  /** `Slug.cases.json` of multi-tenancy, the corpus the Vitest of `tenantUpsertRequestSchema` in patterns admin-ui reads. */
  SlugCases,

  /** `scriptMoney.ts` of script-money, which `TypeScriptCode.fromClasspath` appends to every script. */
  ScriptMoneySource,

  /** `scriptMoney.cases.json` of script-money, the corpus its functions share with the Kotlin money rules. */
  ScriptMoneyCases,
}
