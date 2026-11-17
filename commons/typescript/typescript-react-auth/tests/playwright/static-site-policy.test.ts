/**
 * Imports
 */

import { openStorybookBuildUnderStaticSitePolicy } from "@neckar-hub/playwright-config";
import { expect, test } from "@playwright/test";

/**
 * The showcase image serves `build/storybook/` under the `static-site` policy `script-src 'self'`; an inline script or
 * code evaluated from a string leaves the deployed page blank.
 */
test("the Storybook build renders under the static-site Content Security Policy", async ({ page }) => {
  const problems = await openStorybookBuildUnderStaticSitePolicy(page, "build/storybook");

  expect(problems).toEqual([]);
});
