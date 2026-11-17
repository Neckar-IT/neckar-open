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

A suite against the package's Storybook stories starts from `storybookSuiteConfig`, which adds
baselines, reports and CI traces ([Runs and baselines](#runs-and-baselines)):

```typescript
import { storybookSuiteConfig, storybookWebServer } from "@neckar-hub/playwright-config";
import { defineConfig } from "@playwright/test";

export default defineConfig({
  ...storybookSuiteConfig,
  webServer: storybookWebServer({ basePort: 62000 }),
});
```

A suite against Ladle stories takes `ladleSuiteConfig` and `ladleWebServer({ basePort: 61040 })`, with the
package's slot in `LadleDevPorts`.

Every other setting is overridden in the same place, for example `testDir: "./tests/e2e"`.

## Storybook web server

`storybookWebServer({ basePort })` returns the `webServer` entry that serves the package's Storybook
with `storybook dev`. The `basePort` is the package's slot in `StorybookDevPorts`
([`build-logic/descriptors/src/jvmMain/kotlin/it/neckar/projects/common/StorybookDevPorts.kt`](../../../../../build-logic/descriptors/src/jvmMain/kotlin/it/neckar/projects/common/StorybookDevPorts.kt)),
checked by `./gradlew verifyStorybookPorts`. The Gradle task `integrationTest` overrides it through
the environment variable `STORYBOOK_PORT` with the port of the current worktree.

Test files read the stories of `/index.json` with `storybookStories(url)` and open a story in the preview frame:

```typescript
import { storybookServerUrl, storybookStories } from "@neckar-hub/playwright-config";
import { waitForStorybookStoryReady } from "@neckar-hub/typescript-react-utils/storybook";

const url = storybookServerUrl();
const stories = storybookStories(url); // read synchronously, because Playwright declares the tests before the first one runs

await page.goto(`${url}/iframe.html?id=${story.id}&viewMode=story`);
await waitForStorybookStoryReady(page);
```

`waitForStorybookStoryReady` waits for the flag `useStorybookStoryReadyFlag` sets in a decorator of
`.storybook/preview.tsx`. `storybookServerUrl()` throws when `STORYBOOK_PORT` is unset — that means
the package's `playwright.config.ts` does not use `storybookWebServer`.

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
import { openStorybookBuildUnderStaticSitePolicy } from "@neckar-hub/playwright-config";

const problems = await openStorybookBuildUnderStaticSitePolicy(page, "build/storybook");
expect(problems).toEqual([]);
```

`openStorybookBuildUnderStaticSitePolicy` and `openLadleBuildUnderStaticSitePolicy` serve the
`build/storybook/` and `build/ladle/` output with the policy header of the
`static-site` base image, rendered from [`content-security-policy-header.conf.template`](../../../../infrastructure/common/static-site/templates/includes/content-security-policy-header.conf.template)
and the `CSP_*` defaults of its [`Dockerfile`](../../../../infrastructure/common/static-site/Dockerfile).
They return every page error and every Content Security Policy violation until the first story
rendered and the network was idle, and throw with them when no story renders within 15 s or the
story viewer shows its error page. `storybookBuild` moves the inline scripts of `build/storybook/`
into files beside their pages, because `script-src 'self'` blocks the inline scripts `storybook build`
writes.

`integrationTest` depends on `storybookBuild` or `ladleBuild` in every package that has a
`storybook:build` or `ladle:build` script and an `integration-test` script.

## Upload of a test data file

```typescript
import { testDataFile } from "@neckar-hub/playwright-config";

await page.locator("input[type=file]").setInputFiles(testDataFile("author-portraits/portrait-senior-1024x1024.webp"));
```

`testDataFile` returns the absolute path below `test-data/` at the root of the Git repository and
throws for a missing file. A renamed file under `test-data/` therefore fails every test that names it.

## Runs and baselines

```bash
git fetch origin data/storybook-baselines                  # the baselines, part of every plain `git fetch`
./gradlew storybookTests                                   # every package with a Storybook suite
./gradlew :internal:open:commons:typescript:typescript-react-ui:integrationTest
./gradlew storybookTests -Pplaywright.updateSnapshots=all  # rewrite them into build/storybook-baselines/
./gradlew ladleTests                                       # every package with a Ladle suite
```

`storybookTests` exists in every package that has a `storybook` and an `integration-test` script,
`ladleTests` in every package with a `ladle` and an `integration-test` script. The schedule
`run-storybook-tests` runs `storybookTests` nightly on `main`
([`gitlab-ci.d/schedule/run-storybook-tests.yml`](../../../../../gitlab-ci.d/schedule/run-storybook-tests.yml)),
`run-ladle-tests` runs `ladleTests`
([`gitlab-ci.d/schedule/run-ladle-tests.yml`](../../../../../gitlab-ci.d/schedule/run-ladle-tests.yml)).
A merge request offers them as the manual jobs `✋ run the Storybook tests` and `✋ run the Ladle tests`.

| Story viewer | Baseline branch | Extract task | `snapshotDir` |
|---|---|---|---|
| Storybook | `data/storybook-baselines` | `extractStorybookBaselines` | `build/storybook-baselines/` |
| Ladle | `data/ladle-baselines` | `extractLadleBaselines` | `build/ladle-baselines/` |

A baseline branch holds one commit without history, with
`<package path>/screenshots.test.ts-snapshots/<screenshot name>-chromium-linux.png`. Before a run, the
extract task unpacks the package's part of the branch into the `snapshotDir`.

The baselines are the pixels of the CI image `gitlab-ci-runner` with the Chromium that
`@playwright/test` in `pnpm-lock.yaml` fixes. A workstation with other fonts renders monospace text
differently. The manual jobs `✋ update the Storybook baselines` and `✋ update the Ladle baselines` (merge request
and `main` pipeline) rewrite every baseline of their story viewer and, after a green run, replace its branch by a force push
([`gitlab-ci.d/scripts/publish-story-baselines.sh`](../../../../../gitlab-ci.d/scripts/publish-story-baselines.sh)).
A merge request that changes stories runs the job before the merge; a red nightly after a merge is
answered by the same job in a `main` pipeline.

`-Pplaywright.updateSnapshots` takes Playwright's `all`, `changed`, `missing` or `none`. With `all`
the extract task leaves the `snapshotDir` empty, so a baseline without a story disappears.

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
