/**
 * Imports
 */

import { ladleServerUrl } from "@neckar-hub/playwright-config";
import { expect, test } from "@playwright/test";

/**
 * Configuration
 */

const url = ladleServerUrl(); // the story browser this run serves — port from the package's Ladle slot

/**
 * The Ladle HTML shell ships with @ladle/react — there is no index.html in this package that could
 * carry the serve-time placeholders every other Neckar IT frontend uses, so the head block comes
 * from `appendToHead` in .ladle/config.mjs. This test guards that the image digest tag reaches the
 * head: a missing tag means the deployed showcase could no longer be mapped to its image.
 *
 * A local dev server runs no image, so the placeholder stays for the container to fill — unless the
 * environment of the run sets a digest, which the dev server then fills in.
 */
test("head meta tags carry the image digest placeholder", async ({ page }) => {
  await page.goto(url);

  const imageDigest = await page.locator('head meta[name="imageDigest"]').getAttribute("content");

  expect(imageDigest).toMatch(/^(\$\{APP_IMAGE_DIGEST\}|sha256:[a-f0-9]{64})$/);
});

/**
 * The runtime context travels the same channel, and its three tags are the ones a CSP once silenced
 * without anything noticing. Locally they resolve to what the Gradle `run` tasks give the backend.
 */
test("head meta tags carry the injected runtime context", async ({ page }) => {
  await page.goto(url);

  const executionEnvironment = await page.locator('head meta[name="runtime-context.executionEnvironment"]').getAttribute("content");
  const deploymentStage = await page.locator('head meta[name="runtime-context.deploymentStage"]').getAttribute("content");
  const serviceHost = await page.locator('head meta[name="runtime-context.serviceHost"]').getAttribute("content");

  expect(executionEnvironment).toBe("LocalDev");
  expect(deploymentStage).toBe("Development");
  expect(serviceHost).toBe("localhost");
});
