import type { Page } from "@playwright/test";
import { afterEach, describe, expect, it } from "vitest";
import { isTestMode } from "./isTestMode";
import { setTestMode } from "./setTestMode";

/**
 * Runs `setTestMode` against a page stub and executes the init script it registers, so the property
 * the Playwright side writes and the property the browser side reads are compared against each other.
 */
async function applySetTestModeToCurrentWindow(testModeValue: boolean): Promise<void> {
  let registeredInitScript: ((argument: unknown) => void) | undefined;
  let registeredArgument: unknown;

  const page = {
    addInitScript(script: (argument: unknown) => void, argument: unknown) {
      registeredInitScript = script;
      registeredArgument = argument;
      return Promise.resolve();
    },
  } as unknown as Page;

  await setTestMode(page, testModeValue);

  registeredInitScript?.(registeredArgument);
}

describe("isTestMode()", () => {
  afterEach(() => {
    window.testMode = undefined;
  });

  it("is false when nothing set the flag", () => {
    expect(isTestMode()).toBe(false);
  });

  it("reads the flag that setTestMode writes", async () => {
    await applySetTestModeToCurrentWindow(true);

    expect(isTestMode()).toBe(true);
  });

  it("reads a disabled test mode that setTestMode writes", async () => {
    await applySetTestModeToCurrentWindow(false);

    expect(isTestMode()).toBe(false);
  });
});
