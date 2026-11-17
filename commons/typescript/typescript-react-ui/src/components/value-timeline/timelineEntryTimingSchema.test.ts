import { findOptionalFormSchemaFieldsRejectingEmptyValue } from "@neckar-hub/typescript-utils/zod-utils";
import { describe, expect, it } from "vitest";
import { timelineEntryTimingSchema } from "./timelineEntryTimingSchema";

describe("timelineEntryTimingSchema", () => {
  it("accepts every encoding of an empty scheduled date", () => {
    // `DateTimePickerFormField` reports a cleared date as `null`, a page that never touched the
    // field leaves it `undefined`. Rejecting either blocks the submit of the whole form.
    expect(timelineEntryTimingSchema.safeParse({ type: "scheduled", validFrom: null }).success).toBe(true);
    expect(timelineEntryTimingSchema.safeParse({ type: "scheduled", validFrom: undefined }).success).toBe(true);
    expect(timelineEntryTimingSchema.safeParse({ type: "scheduled" }).success).toBe(true);
  });

  it("keeps the timing choice mandatory", () => {
    expect(timelineEntryTimingSchema.safeParse({ validFrom: null }).success).toBe(false);
  });

  it("has no optional field that rejects one of the two empty encodings", () => {
    expect(findOptionalFormSchemaFieldsRejectingEmptyValue(timelineEntryTimingSchema)).toEqual([]);
  });
});
