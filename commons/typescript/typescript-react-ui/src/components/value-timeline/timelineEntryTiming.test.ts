import { describe, expect, it } from "vitest";
import { timelineEntryTiming } from "./timelineEntryTiming";

describe("timelineEntryTiming", () => {
  it("maps an entry taking effect now and drops a picked date", () => {
    expect(timelineEntryTiming({ type: "now", validFrom: new Date("2026-12-01T08:00:00Z") })).toEqual({ type: "now" });
  });

  it("maps a scheduled entry onto the ISO instant", () => {
    expect(timelineEntryTiming({ type: "scheduled", validFrom: new Date("2026-12-01T08:00:00Z") })).toEqual({
      type: "scheduled",
      validFrom: "2026-12-01T08:00:00.000Z",
    });
  });

  it("refuses a scheduled entry without validFrom", () => {
    expect(() => timelineEntryTiming({ type: "scheduled", validFrom: null })).toThrow("useTimelineEntryFormResolver");
  });
});
