# internal/open

Open-source Kotlin and TypeScript libraries, published to Maven Central via
`com.vanniktech.maven.publish` (GPLv3 with Classpath Exception; `meistercharts` is Apache 2.0).

Membership in `internal/open/` is the publish boundary: a module lives here because it is
published as open source.

## GitHub mirror

```bash
git clone https://github.com/Neckar-IT/neckar-open.git && cd neckar-open && git lfs pull && ./gradlew build
```

https://github.com/Neckar-IT/neckar-open is a read-only mirror of every file git tracks under `internal/open`, plus the build setup that `tools/standalone-build/export-standalone.sh` writes for a build outside the monorepo, such as the Gradle wrapper and `gradle.properties`. Pull requests there are not merged; changes go into this monorepo.

Decision: ADL 0236.

| Branch or tag | Content | Writer |
|---|---|---|
| `main` | the newest release, built by `tools/standalone-build/verify-standalone.sh` before the push | `🏷️ Publish open Release to GitHub` on each tag `open-*` |
| tag `open-*` | the state of the monorepo tag of the same name | `🏷️ Publish open Release to GitHub` |
| `develop` | the current state of `main` of the monorepo, unbuilt; fails `./gradlew build` while `internal/open` uses an unreleased change of `internal/buildplatform` | `⏲ publish the open sources to GitHub`, daily at 04:00 in the schedule `Publish Sonatype Snapshots / Publish Meistercharts to GitHub / Publish Open to GitHub` |

The GitHub Action [`.github/workflows/build.yml`](.github/workflows/build.yml) runs `./gradlew build` on `main` and each tag. A red run on `main` opens an issue in neckar-hub; the checks before each push are listed in the header of [`gitlab-ci.d/scripts/publish-sources-to-github.sh`](../../gitlab-ci.d/scripts/publish-sources-to-github.sh).

## Structure

| Directory | Content |
|-----------|---------|
| `commons/` | General-purpose utility libraries (`it.neckar.open.*`) — collections, time, i18n, money, geometry, IO, serialization helpers, React/TypeScript UI, … |
| `contact/`, `user/`, `customer/` | Shared domain models (`it.neckar.contact`, `it.neckar.user`, `it.neckar.customer`) — phone number, email address, the user, the postal address and company master data |
| `business-domain/`, `financial/` | Tags, catalog items, edit history, value timelines (`it.neckar.tag`, `it.neckar.catalog`, `it.neckar.price`, `it.neckar.timeline`, `it.neckar.worktype`) and quote arithmetic (`it.neckar.financial`) |
| `auth-rest-api/` | The wire format of an authentication service (`it.neckar.auth.rest`) |
| `rest/` | Ktor client/server layer, REST model, OpenAPI generator |
| `unit/` | Units of measure and conversions |
| `ksp/` | Published KSP processors (serialization/boxing verifiers, TS declaration generator, …) |
| `integrations/` | External API clients (Mattermost) |
| `meistercharts/` | MeisterCharts charting library (Apache 2.0) |
| `annotations/` | Cross-cutting annotations (`it.neckar.open.annotations`) |
| `detekt/` | Custom Detekt rules |
| `version-info/` | Build/version metadata library |
| `web-resources/` | Shared web fonts and resources |
| `demos/` | Runnable demos for the libraries above (not published) |

Publish configuration lives in `build.gradle.kts`. Module paths are registered in the
repository-root `settings.gradle.kts` and in `internal/open/project-registry/open-projects/src/main/kotlin/it/neckar/projects/OpenProjects.kt`,
nested along the directories: `Projects.open.commons.kotlinLang`. The MeisterCharts modules are registered in
`internal/open/meistercharts/project-registry/meistercharts-projects/src/main/kotlin/it/neckar/projects/MeisterchartsProjects.kt`.

The snapshot, release, and setup workflow is documented in
[`docs/maven-central-publishing.md`](docs/maven-central-publishing.md).
