import { afterEach, describe, expect, it, vi } from "vitest";
import { configDefaults } from "vitest/config";
import type { sharedVitestTestOptions } from "./vitest-config.js";

type SharedVitestTestOptions = typeof sharedVitestTestOptions;

// The constant is evaluated at module load, so each case needs its own evaluation under a stubbed CI.
async function optionsWithCi(ci: string | undefined): Promise<SharedVitestTestOptions> {
  vi.stubEnv("CI", ci);
  vi.resetModules();
  const { sharedVitestTestOptions } = await import("./vitest-config.js");
  return sharedVitestTestOptions;
}

async function reportersWithCi(ci: string | undefined): Promise<string[]> {
  return (await optionsWithCi(ci)).reporters;
}

describe("sharedVitestTestOptions.reporters", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("takes the dot reporter in CI", async () => {
    expect(await reportersWithCi("true")).toEqual(["dot"]);
  });

  it("leaves the reporter to Vitest outside CI", async () => {
    expect(await reportersWithCi(undefined)).toEqual(configDefaults.reporters);
  });

  it("does not alias Vitest's own defaults array", async () => {
    expect(await reportersWithCi(undefined)).not.toBe(configDefaults.reporters);
  });
});

describe("sharedVitestTestOptions.testTimeout", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("allows 30 s per test in CI", async () => {
    expect((await optionsWithCi("true")).testTimeout).toBe(30_000);
  });

  it("keeps Vitest's 5 s outside CI", async () => {
    expect((await optionsWithCi(undefined)).testTimeout).toBe(5_000);
  });
});

describe("sharedVitestTestOptions.detectAsyncLeaks", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("is off for every package", async () => {
    expect((await optionsWithCi(undefined)).detectAsyncLeaks).toBe(false);
  });
});

describe("sharedVitestTestOptions.isolate", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("is off for every package", async () => {
    expect((await optionsWithCi(undefined)).isolate).toBe(false);
  });
});
