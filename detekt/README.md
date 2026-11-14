# neckar-detekt

Custom Detekt rules for this codebase. Rules live in
[`src/main/kotlin/it/neckar/detekt/`](src/main/kotlin/it/neckar/detekt/),
register themselves via [`NeckarRulesProvider`](src/main/kotlin/it/neckar/detekt/NeckarRulesProvider.kt),
and are enabled per-rule under `neckar-rules:` in [`internal/buildplatform/config/detekt/detekt.yml`](../../buildplatform/config/detekt/detekt.yml).

## What belongs here

Every rule about Kotlin source is a rule of this ruleset, and it reads the syntax tree. A verdict drawn
from the source text by a regular expression follows the spelling: an import alias, a line break inside a
call, a value bound to a variable and the construct's name inside a comment each move it for one unchanged
meaning.

The boundary runs along the subject of the rule:

| Subject | Where |
|---|---|
| a Kotlin construct — expression, declaration, import, annotation | a rule in this module |
| file text across several languages (`kt`, `ts`, `sh`, `py`, `toml`, …) | a repository check in `build-logic/plugins/` |
| build file, YAML, deployment descriptor, repository structure | a repository check in `build-logic/plugins/` |

A rule names its own subject — the import a file carries, the annotation on the declaration, the
package it sits in. No module opts into a rule from its `build.gradle.kts`.

A construct the architecture permits against a rule carries `@Suppress("RuleName")` with the reason on
the line above. It sits on the declaration that holds the construct, or on the statement itself where a
declaration would reach further than the one construct. The suppression covers what it sits on and
everything inside it; a sibling statement stays reported. Every other rule keeps its reach.

## Adding a rule

1. Create the class extending `dev.detekt.api.Rule`. If type information is needed
   (resolved supertypes, function signatures, call targets), add `RequiresAnalysisApi`.
2. Add `::YourRule` to the list in `NeckarRulesProvider.instance()`.
3. Add a `YourRule: { active: true }` entry under `neckar-rules:` in `internal/buildplatform/config/detekt/detekt.yml`.

## Running rules

Rules with `RequiresAnalysisApi` need full Kotlin type resolution. They run only in
the **per-source-set** detekt tasks — `detektMain`, `detektTest`, `detektPdfExamples`,
etc. — **not** in the `detekt` aggregator task.

```bash
# Right (Analysis-API rules fire):
./gradlew :internal:patterns:ktor-backend:detektMain

# Wrong (no type resolution → RequiresAnalysisApi rules silently skipped):
./gradlew :internal:patterns:ktor-backend:detekt
```

If you debug a rule that "doesn't fire", check this first.

## Analysis-API rules are silent right now

Detekt 2.0.0-alpha.6 fails type resolution on this codebase. A `:detektMain` run prints

```
There were N compiler errors found during analysis. This affects accuracy of reporting.
```

and every rule that does its work inside `analyze { }` then reports nothing — not "fewer
findings", nothing at all. `DomainMustNotReferenceDataView` and its layer-separation siblings
are in that group; a deliberate violation passes them unnoticed. The build stays green either
way, because `failOnSeverity` is `Never` in this state (see
[`config/detekt/README.md`](../../../config/detekt/README.md)).

The PSI half of a rule keeps working. `ForbiddenPackageDependency` reports an import that
crosses a configured boundary in the same run that prints the message above — its
`visitImportDirective` needs no type information. Only its second half, which resolves a
fully-qualified inline reference, is out.

A Detekt release that loads compiler plugins switches type resolution back on, and every rule
resumes with it: [detekt#7531](https://github.com/detekt/detekt/issues/7531), upstream state and
repository steps in [`config/detekt/README.md`](../../../config/detekt/README.md#upstream-stand-vom-2026-11-03).
Until then, write a rule so that whatever it can decide from the PSI alone
is decided there, and reach for `analyze { }` for the part that genuinely needs a resolved type —
that part will simply start working later. Tracked in
[#1957](https://git.neckar.it/neckarit/neckar-hub/-/issues/1957).

## Verifying a rule

A rule carries a test in [`src/test/kotlin/it/neckar/detekt/`](src/test/kotlin/it/neckar/detekt/)
that lints a source snippet and asserts over the findings. `dev.detekt.test.lint` takes a rule
deciding from the syntax tree, `dev.detekt.test.lintWithContext` one carrying `RequiresAnalysisApi`
and a `createEnvironment()`. `./gradlew :internal:open:detekt:test` runs them.

`lint` resolves `@Suppress("RuleName")` through the same parent walk as a real run, so a test pins the
reach described above.

Over real sources:

1. **Positive case** — point `:detektMain` at code that conforms (e.g. for the tenant rules,
   `internal/patterns/ktor-backend` is the reference implementation): expect 0 findings.
2. **Negative case** — introduce a deliberate violation in one commit, run `:detektMain`,
   confirm the report under `build/reports/detekt/` names it, revert. The task stays green
   while `failOnSeverity` is `Never`.
3. **Full-repo sweep** — `./gradlew detektAll` runs every Detekt task adding coverage across
   every subproject and surfaces existing violations elsewhere.

## Self-detekt

This module cannot run detekt on itself: the `neckar-rules` ruleset lives here and is
loaded as a `detektPlugins` dep by every other project. Self-running would be a
circular reference. The detekt task is therefore disabled in this module's
`build.gradle.kts`.
