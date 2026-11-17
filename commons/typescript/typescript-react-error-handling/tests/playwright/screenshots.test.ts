/**
 * Imports
 */

import { type StorybookStory, storybookServerUrl, storybookStories } from "@neckar-hub/playwright-config";
import { setTestMode } from "@neckar-hub/typescript-react-utils/set-test-mode";
import { waitForStorybookStoryReady } from "@neckar-hub/typescript-react-utils/storybook";
import { expect, type Page, test } from "@playwright/test";

/**
 * Configuration
 */

const url = storybookServerUrl(); // the Storybook this run serves — port from the package's Storybook slot
const stories = storybookStories(url);

const screenshotOptions = { fullPage: true };

/**
 * Opens the story in the preview frame with test mode on, which disables features in components that break a screenshot.
 */
async function openStory(page: Page, story: StorybookStory, colorMode: "light" | "dark"): Promise<void> {
  await setTestMode(page, true);
  await page.goto(`${url}/iframe.html?id=${story.id}&viewMode=story&globals=theme:${colorMode}`);
  await waitForStorybookStoryReady(page);
}

/**
 * Compares every story in the light and the dark color mode, and a story tagged `responsive` also at phone and tablet width.
 * A story tagged `no-screenshot` is skipped.
 */
stories.forEach((story) => {
  test(`${story.id} - compare screenshots for light theme`, async ({ page }) => {
    test.skip(story.tags.includes("no-screenshot"), "tagged no-screenshot");

    await openStory(page, story, "light");
    await expect(page).toHaveScreenshot(`${story.id}--light.png`, screenshotOptions);
  });

  test(`${story.id} - compare screenshots for dark theme`, async ({ page }) => {
    test.skip(story.tags.includes("no-screenshot"), "tagged no-screenshot");

    await openStory(page, story, "dark");
    await expect(page).toHaveScreenshot(`${story.id}--dark.png`, screenshotOptions);
  });

  if (story.tags.includes("responsive")) {
    test(`${story.id} - compare screenshots for mobile view`, async ({ page }) => {
      test.skip(story.tags.includes("no-screenshot"), "tagged no-screenshot");

      await page.setViewportSize({ width: 390, height: 844 }); // screen resolution for mobile devices
      await openStory(page, story, "light");
      await expect(page).toHaveScreenshot(`${story.id}--mobile.png`, screenshotOptions);
    });

    test(`${story.id} - compare screenshots for tablet view`, async ({ page }) => {
      test.skip(story.tags.includes("no-screenshot"), "tagged no-screenshot");

      await page.setViewportSize({ width: 800, height: 1280 }); // screen resolution for tablets
      await openStory(page, story, "light");
      await expect(page).toHaveScreenshot(`${story.id}--tablet.png`, screenshotOptions);
    });
  }
});
