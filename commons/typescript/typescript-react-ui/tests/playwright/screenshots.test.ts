/* eslint-disable @typescript-eslint/strict-boolean-expressions */

/**
 * Imports
 */

import { ladleServerUrl } from "@neckar-hub/playwright-config";
import { waitForLadleStoryReady } from "@neckar-hub/typescript-react-utils/ladle";
import { setTestMode } from "@neckar-hub/typescript-react-utils/set-test-mode";
import { expect, test } from "@playwright/test";
import fetch from "sync-fetch"; // we can't create tests asynchronously, thus using the sync-fetch lib

/**
 * Configuration
 */

const url = ladleServerUrl(); // the story browser this run serves — port from the package's Ladle slot
const stories = fetch(`${url}/meta.json`).json().stories; // fetch Ladle's meta file

const screenshotOptions = { fullPage: true };

/**
 * Iterate trough stories and compare screenshots for light and dark theme
 */

Object.keys(stories).forEach((storyKey) => {
  test(`${storyKey} - compare screenshots for light theme`, async ({ page }) => {
    test.skip(stories[storyKey].meta.skip, "meta.skip is true"); // skip stories with `meta.skip` set to true

    await setTestMode(page, true); // set testMode to disable certain features in components that may break the screenshot tests
    await page.goto(`${url}/?story=${storyKey}&mode=preview`);
    await waitForLadleStoryReady(page);
    await expect(page).toHaveScreenshot(`${storyKey}--light.png`, screenshotOptions);
  });

  test(`${storyKey} - compare screenshots for dark theme`, async ({ page }) => {
    test.skip(stories[storyKey].meta.skip, "meta.skip is true"); // skip stories with `meta.skip` set to true

    await setTestMode(page, true); // set testMode to disable certain features in components that may break the screenshot tests

    await page.goto(`${url}/?story=${storyKey}&mode=preview&theme=dark`);
    await waitForLadleStoryReady(page);
    await expect(page).toHaveScreenshot(`${storyKey}--dark.png`, screenshotOptions);
  });

  if (stories[storyKey].meta.responsive) {
    test(`${storyKey} - compare screenshots for mobile view`, async ({ page }) => {
      test.skip(stories[storyKey].meta.skip, "meta.skip is true"); // skip stories with `meta.skip` set to true

      await setTestMode(page, true); // set testMode to disable certain features in components that may break the screenshot tests

      await page.setViewportSize({ width: 390, height: 844 }); // screen resolution for mobile devices
      await page.goto(`${url}/?story=${storyKey}&mode=preview`);
      await waitForLadleStoryReady(page);
      await expect(page).toHaveScreenshot(`${storyKey}--mobile.png`, screenshotOptions);
    });

    test(`${storyKey} - compare screenshots for tablet view`, async ({ page }) => {
      test.skip(stories[storyKey].meta.skip, "meta.skip is true"); // skip stories with `meta.skip` set to true

      await setTestMode(page, true); // set testMode to disable certain features in components that may break the screenshot tests

      await page.setViewportSize({ width: 800, height: 1280 }); // screen resolution for tablets
      await page.goto(`${url}/?story=${storyKey}&mode=preview`);
      await waitForLadleStoryReady(page);
      await expect(page).toHaveScreenshot(`${storyKey}--tablet.png`, screenshotOptions);
    });
  }
});
