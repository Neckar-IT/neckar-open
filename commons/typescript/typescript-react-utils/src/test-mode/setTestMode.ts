/**
 * Imports
 */

import { type Page } from "@playwright/test";
import { testModeWindowProperty } from "./isTestMode";

/**
 * Set a `testMode` property on the global window object.
 * This property can be read with the `isTestMode` function and can be used to disable certain features in the
 * production code that would interfere with the screenshot tests.
 */

export async function setTestMode(page: Page, testModeValue: boolean): Promise<void> {
  // The init script is serialized into the browser and closes over nothing — the property name
  // travels as an argument so that both sides keep using the same constant.
  await page.addInitScript(
    ({ property, value }: { property: string; value: boolean }) => {
      Object.defineProperty(window, property, {
        value,
        writable: true,
        configurable: true,
      });
    },
    { property: testModeWindowProperty, value: testModeValue },
  );
}
