# Playwright Config

Shareable configuration for Playwright screenshot tests.

## Usage

Declare the dependency in the `package.json` of your package:

```json
{
  "devDependencies": {
    "@neckar-hub/playwright-config": "workspace:*"
  }
}
```

Then create a `playwright.config.ts` in the root of your package. `sharedConfig` brings the test
directory (`./tests/playwright`), the Chromium project with the `de` locale, the reporter and the
retry count. It carries no `webServer` on purpose — a suite either drives a story browser or the
product's own dev server, so every package adds the one that serves its pages.

A suite against the package's Ladle stories starts from `ladleSuiteConfig`, which adds baselines,
reports and CI traces ([Runs and baselines](#runs-and-baselines)):

```typescript
import { ladleSuiteConfig, ladleWebServer } from "@neckar-hub/playwright-config";
import { defineConfig } from "@playwright/test";

export default defineConfig({
  ...ladleSuiteConfig,
  webServer: ladleWebServer({ basePort: 61000 }),
});
```

Every other setting is overridden in the same place, for example `testDir: "./tests/e2e"`.

## Ladle web server

`ladleWebServer({ basePort })` returns the `webServer` entry that serves the package's Ladle
stories. The `basePort` is the package's slot in `LadleDevPorts`
([`build-logic/descriptors/src/jvmMain/kotlin/it/neckar/projects/common/LadleDevPorts.kt`](../../../../../build-logic/descriptors/src/jvmMain/kotlin/it/neckar/projects/common/LadleDevPorts.kt)),
checked by `./gradlew verifyLadlePorts`. The Gradle task `integrationTest` overrides it through the
environment variable `LADLE_PORT` with the port of the current worktree, so screenshot runs in
parallel worktrees neither share a server nor fight over the port.

Test files that need the URL themselves — to fetch Ladle's `/meta.json` before the first test runs,
for instance — get it from `ladleServerUrl()`:

```typescript
import { ladleServerUrl } from "@neckar-hub/playwright-config";

const url = ladleServerUrl();
const stories = fetch(`${url}/meta.json`).json().stories;
```

`ladleServerUrl()` throws when `LADLE_PORT` is unset — that means the package's
`playwright.config.ts` does not use `ladleWebServer`.

## Built showcase under the Content Security Policy

```typescript
import { openLadleBuildUnderStaticSitePolicy } from "@neckar-hub/playwright-config";

const problems = await openLadleBuildUnderStaticSitePolicy(page, "build/ladle");
expect(problems).toEqual([]);
```

`openLadleBuildUnderStaticSitePolicy` serves the `ladle build` output with the policy header of the
`static-site` base image, rendered from [`content-security-policy-header.conf.template`](../../../../infrastructure/common/static-site/templates/includes/content-security-policy-header.conf.template)
and the `CSP_*` defaults of its [`Dockerfile`](../../../../infrastructure/common/static-site/Dockerfile).
It returns every page error and every Content Security Policy violation until the story rendered and
the network was idle, and throws with them when no story renders within 15 s or Ladle shows an
error page.

`integrationTest` depends on `ladleBuild` in every package that has both a `ladle:build` and an
`integration-test` script.

## Upload of a test data file

```typescript
import { testDataFile } from "@neckar-hub/playwright-config";

await page.locator("input[type=file]").setInputFiles(testDataFile("author-portraits/portrait-senior-1024x1024.webp"));
```

`testDataFile` returns the absolute path below `test-data/` at the root of the Git repository and
throws for a missing file. A renamed file under `test-data/` therefore fails every test that names it.

## Runs and baselines

```bash
git fetch origin data/ladle-baselines                      # the baselines, part of every plain `git fetch`
./gradlew ladleTests                                       # every package with a Ladle suite
./gradlew :internal:open:commons:typescript:typescript-react-auth:integrationTest
./gradlew ladleTests -Pplaywright.updateSnapshots=all      # rewrite them into build/ladle-baselines/
```

`ladleTests` exists in every package that has a `ladle` and an `integration-test` script. The
schedule `run-ladle-tests` runs it nightly on `main`
([`gitlab-ci.d/schedule/run-ladle-tests.yml`](../../../../../gitlab-ci.d/schedule/run-ladle-tests.yml)),
a merge request offers it as the manual job `✋ run the Ladle tests`.

The baselines are on the branch `data/ladle-baselines`, one commit without history, as
`<package path>/screenshots.test.ts-snapshots/<screenshot name>-chromium-linux.png`. Before a run,
`extractLadleBaselines` unpacks the package's part of `origin/data/ladle-baselines` into
`build/ladle-baselines/`, the `snapshotDir` of `ladleSuiteConfig`.

The baselines are the pixels of the CI image `gitlab-ci-runner` with the Chromium that
`@playwright/test` in `pnpm-lock.yaml` fixes. A workstation with other fonts renders monospace text
differently. The manual job `✋ update the Ladle baselines` (merge request and `main` pipeline)
rewrites every baseline and, after a green run, replaces the branch by a force push
([`gitlab-ci.d/scripts/publish-ladle-baselines.sh`](../../../../../gitlab-ci.d/scripts/publish-ladle-baselines.sh)).
A merge request that changes stories runs it before the merge; a red nightly after a merge is
answered by the same job in a `main` pipeline.

`-Pplaywright.updateSnapshots` takes Playwright's `all`, `changed`, `missing` or `none`. With `all`
`extractLadleBaselines` leaves `build/ladle-baselines/` empty, so a baseline without a story
disappears.

Every run writes its results below the package's `build/`: the JUnit file
`build/test-results/integrationTest/TEST-playwright.xml` (read by `build-result.json`), the actual,
expected and diff images of a failed comparison in `build/playwright/test-results/`, the HTML report
in `build/reports/playwright/`.

## Disable certain features for screenshot tests

Sometimes a feature has to be off while a screenshot is taken (an animation, a countdown). The test
sets the test mode on the page, the component reads it. Both helpers live in
`@neckar-hub/typescript-react-utils`, not in this package.

In the test:

```typescript
import { setTestMode } from "@neckar-hub/typescript-react-utils/set-test-mode";

test("screenshot test", async ({ page }) => {
  await setTestMode(page, true);
  // …
});
```

And in the component:

```typescript
import { isTestMode } from "@neckar-hub/typescript-react-utils/is-test-mode";

const testMode = isTestMode();
```
