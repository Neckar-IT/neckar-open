/**
 * Imports
 */

import type { PriceTimelineEntryView, PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { describe, expect, it } from "vitest";
import { classifyTimelineEntry } from "./classifyTimelineEntry";

/**
 * Tests
 */

describe("classifyTimelineEntry", () => {
  const now = new Date("2026-06-01T00:00:00Z");

  const entry = (validFrom: string, price = 1000): PriceTimelineEntryView => ({ price, validFrom });

  const summaryWithCurrent = (current: PriceTimelineEntryView | null): PriceTimelineSummary => ({
    currentEntry: current,
    nextFutureEntry: null,
  });

  it("classifies an entry with validFrom after now as future", () => {
    const future = entry("2026-07-01T00:00:00Z");

    expect(classifyTimelineEntry(future, summaryWithCurrent(null), now)).toBe("future");
  });

  it("classifies the summary's current entry as current", () => {
    const current = entry("2026-05-01T00:00:00Z");

    expect(classifyTimelineEntry(current, summaryWithCurrent(current), now)).toBe("current");
  });

  it("classifies a past entry that is not the current entry as past", () => {
    const past = entry("2026-04-01T00:00:00Z");
    const current = entry("2026-05-01T00:00:00Z");

    expect(classifyTimelineEntry(past, summaryWithCurrent(current), now)).toBe("past");
  });

  it("classifies a past entry as past when the summary has no current entry", () => {
    const past = entry("2026-04-01T00:00:00Z");

    expect(classifyTimelineEntry(past, summaryWithCurrent(null), now)).toBe("past");
  });

  it("treats an entry whose validFrom equals now as current when it is the marked current entry", () => {
    const atNow = entry("2026-06-01T00:00:00Z");

    expect(classifyTimelineEntry(atNow, summaryWithCurrent(atNow), now)).toBe("current");
  });

  it("treats an entry whose validFrom equals now as past when it is not the marked current entry", () => {
    const atNow = entry("2026-06-01T00:00:00Z");
    const otherCurrent = entry("2026-05-01T00:00:00Z");

    expect(classifyTimelineEntry(atNow, summaryWithCurrent(otherCurrent), now)).toBe("past");
  });

  it("matches the current entry by validFrom rather than object identity", () => {
    const entryInstance = entry("2026-05-01T00:00:00Z");
    const currentInstance = entry("2026-05-01T00:00:00Z");

    // Distinct object instances with the same validFrom must still resolve to current.
    expect(classifyTimelineEntry(entryInstance, summaryWithCurrent(currentInstance), now)).toBe("current");
  });
});
