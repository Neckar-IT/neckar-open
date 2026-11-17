/**
 * Imports
 */

import { storybookServerUrl } from "@neckar-hub/playwright-config";
import { expect, test } from "@playwright/test";

/**
 * Configuration
 */

const url = storybookServerUrl(); // the Storybook this run serves — port from the package's Storybook slot

/**
 * The HTML shells ship with Storybook, so the head block comes from `managerHead` and `previewHead` in
 * .storybook/main.ts. A missing image digest tag means the deployed showcase could no longer be mapped to its image.
 * The dev server fills the preview frame's placeholder when the environment of the run sets a digest.
 */
test("head meta tags of the manager page carry the image digest placeholder", async ({ page }) => {
  await page.goto(url);

  const imageDigest = await page.locator('head meta[name="imageDigest"]').getAttribute("content");

  expect(imageDigest).toBe("${APP_IMAGE_DIGEST}");
});

test("head meta tags of the preview frame carry the image digest placeholder", async ({ page }) => {
  await page.goto(`${url}/iframe.html`);

  const imageDigest = await page.locator('head meta[name="imageDigest"]').getAttribute("content");

  expect(imageDigest).toMatch(/^(\$\{APP_IMAGE_DIGEST\}|sha256:[a-f0-9]{64})$/);
});

/**
 * The stories read the runtime context in the preview frame. Locally it resolves to what the Gradle `run` tasks give the
 * backend.
 */
test("head meta tags of the preview frame carry the injected runtime context", async ({ page }) => {
  await page.goto(`${url}/iframe.html`);

  const executionEnvironment = await page.locator('head meta[name="runtime-context.executionEnvironment"]').getAttribute("content");
  const deploymentStage = await page.locator('head meta[name="runtime-context.deploymentStage"]').getAttribute("content");
  const serviceHost = await page.locator('head meta[name="runtime-context.serviceHost"]').getAttribute("content");

  expect(executionEnvironment).toBe("LocalDev");
  expect(deploymentStage).toBe("Development");
  expect(serviceHost).toBe("localhost");
});
