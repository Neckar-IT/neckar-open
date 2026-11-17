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

/**
 * The same helper on a page that violates the policy twice: the evaluated string throws and arrives
 * as a page error, the foreign image is only refused and arrives as a console message.
 */
test("page errors and console violations are reported", async ({ page }) => {
  const problems = await openStorybookBuildUnderStaticSitePolicy(page, "tests/playwright/fixtures/policy-violations");

  expect(problems.some((problem) => problem.startsWith("EvalError"))).toBe(true);
  expect(problems.some((problem) => problem.includes("img-src"))).toBe(true);
});
