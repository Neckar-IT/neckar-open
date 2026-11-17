import { describe, expect, it } from "vitest";
import { createQueryClientWithoutGcTimer } from "./createQueryClientWithoutGcTimer";

describe("createQueryClientWithoutGcTimer", () => {
  it("keeps cache entries of queries and mutations forever", () => {
    const defaultOptions = createQueryClientWithoutGcTimer().getDefaultOptions();

    expect(defaultOptions.queries?.gcTime).toBe(Infinity);
    expect(defaultOptions.mutations?.gcTime).toBe(Infinity);
  });

  it("keeps the defaults of createQueryClient", () => {
    const retry = createQueryClientWithoutGcTimer().getDefaultOptions().queries?.retry;
    if (typeof retry !== "function") throw new Error("retry is expected to be a function");

    expect(retry(1, new Error("failed"))).toBe(true);
    expect(retry(2, new Error("failed"))).toBe(false);
  });
});
