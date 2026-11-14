/**
 * Imports
 */

import { openLadleBuildUnderStaticSitePolicy } from "@neckar-hub/playwright-config";
import { expect, test } from "@playwright/test";

/**
 * The showcase image serves `build/ladle/` under the `static-site` policy `script-src 'self'`; a Ladle
 * start that evaluates code from a string leaves the deployed page blank.
 */
test("the Ladle build renders under the static-site Content Security Policy", async ({ page }) => {
  const problems = await openLadleBuildUnderStaticSitePolicy(page, "build/ladle");

  expect(problems).toEqual([]);
});
