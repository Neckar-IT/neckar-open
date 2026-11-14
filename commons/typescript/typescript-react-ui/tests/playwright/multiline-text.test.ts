/**
 * Imports
 */

import { ladleServerUrl } from "@neckar-hub/playwright-config";
import { waitForLadleStoryReady } from "@neckar-hub/typescript-react-utils/ladle";
import { expect, test } from "@playwright/test";

/**
 * Configuration
 */

const url = ladleServerUrl(); // the story browser this run serves — port from the package's Ladle slot

/**
 * jsdom evaluates no stylesheet, so only a browser shows which rule of the truncation contract decides `white-space`.
 */
test.beforeEach(async ({ page }) => {
  await page.goto(`${url}/?story=text--truncatetextcontent--multiline-text&mode=preview`);
  await waitForLadleStoryReady(page);
});

test("a details list shows the line breaks and search hits of a multiline text", async ({ page }) => {
  const text = page.getByTestId("details-zone").locator("[data-multiline-text]");

  await expect(text).toHaveCSS("white-space", "pre-wrap");
  await expect(text.locator("mark")).toHaveText("Freitag");
});

test("a truncation zone keeps a multiline text on one line with an ellipsis", async ({ page }) => {
  const text = page.getByTestId("truncation-zone").locator("[data-multiline-text]");

  await expect(text).toHaveCSS("white-space", "nowrap");
  await expect(text).toHaveCSS("text-overflow", "ellipsis");
});
