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

/**
 * The same helper on a page that violates the policy twice: the evaluated string throws and arrives
 * as a page error, the foreign image is only refused and arrives as a console message.
 */
test("page errors and console violations are reported", async ({ page }) => {
  const problems = await openLadleBuildUnderStaticSitePolicy(page, "tests/playwright/fixtures/policy-violations");

  expect(problems.some((problem) => problem.startsWith("EvalError"))).toBe(true);
  expect(problems.some((problem) => problem.includes("img-src"))).toBe(true);
});
