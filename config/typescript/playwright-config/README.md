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

## Built showcase under the Content Security Policy

```typescript
import { openStorybookBuildUnderStaticSitePolicy } from "@neckar-hub/playwright-config";

const problems = await openStorybookBuildUnderStaticSitePolicy(page, "build/storybook");
expect(problems).toEqual([]);
```

`openStorybookBuildUnderStaticSitePolicy` serves the `build/storybook/` output with the policy header of the
`static-site` base image, rendered from [`content-security-policy-header.conf.template`](../../../../infrastructure/common/static-site/templates/includes/content-security-policy-header.conf.template)
and the `CSP_*` defaults of its [`Dockerfile`](../../../../infrastructure/common/static-site/Dockerfile).
It returns every page error and every Content Security Policy violation until the first story
rendered and the network was idle, and throws with them when no story renders within 15 s or
Storybook shows its error page. `storybookBuild` moves the inline scripts of `build/storybook/`
into files beside their pages, because `script-src 'self'` blocks the inline scripts `storybook build`
writes.

`integrationTest` depends on `storybookBuild` in every package that has a `storybook:build` and an
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
git fetch origin data/storybook-baselines                  # the baselines, part of every plain `git fetch`
./gradlew storybookTests                                   # every package with a Storybook suite
./gradlew :internal:open:commons:typescript:typescript-react-ui:integrationTest
./gradlew storybookTests -Pplaywright.updateSnapshots=all  # rewrite them into build/storybook-baselines/
```

`storybookTests` exists in every package that has a `storybook` and an `integration-test` script. The schedule
`run-storybook-tests` runs `storybookTests` nightly on `main`
([`gitlab-ci.d/schedule/run-storybook-tests.yml`](../../../../../gitlab-ci.d/schedule/run-storybook-tests.yml)).
A merge request offers the suites as the manual job `✋ run the Storybook tests`.

The branch `data/storybook-baselines` holds one commit without history, with
`<package path>/screenshots.test.ts-snapshots/<screenshot name>-chromium-linux.png`. Before a run,
`extractStorybookBaselines` unpacks the package's part of the branch into `build/storybook-baselines/`.

The baselines are the pixels of the CI image `gitlab-ci-runner` with the Chromium that
`@playwright/test` in `pnpm-lock.yaml` fixes. A workstation with other fonts renders monospace text
differently. The manual job `✋ update the Storybook baselines` (merge request
and `main` pipeline) rewrites every Storybook baseline and, after a green run, replaces `data/storybook-baselines` by a force push
([`gitlab-ci.d/scripts/publish-story-baselines.sh`](../../../../../gitlab-ci.d/scripts/publish-story-baselines.sh)).
A merge request that changes stories runs the job before the merge; a red nightly after a merge is
answered by the same job in a `main` pipeline.

`-Pplaywright.updateSnapshots` takes Playwright's `all`, `changed`, `missing` or `none`. With `all`
`extractStorybookBaselines` leaves `build/storybook-baselines/` empty, so a baseline without a story disappears.

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
