import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { timelineEntryTimingSchema } from "./timelineEntryTimingSchema";
import { useTimelineEntryFormResolver } from "./useTimelineEntryFormResolver";

const schema = timelineEntryTimingSchema.extend({ price: z.number() });

async function resolve(values: z.input<typeof schema>) {
  const wrapper = ({ children }: { children: React.ReactNode }) => <I18nProvider options={{ lng: "de" }}>{children}</I18nProvider>;
  const resolver = renderHook(() => useTimelineEntryFormResolver(schema), { wrapper }).result.current;
  return resolver(values, undefined, { fields: {}, shouldUseNativeValidation: false });
}

describe("useTimelineEntryFormResolver", () => {
  it("reports a scheduled entry without validFrom at validFrom", async () => {
    const result = await resolve({ type: "scheduled", validFrom: null, price: 500 });

    expect(result.errors.validFrom?.message).toBe("Bitte Datum und Uhrzeit auswählen.");
  });

  it("accepts a scheduled entry with validFrom", async () => {
    const result = await resolve({ type: "scheduled", validFrom: new Date("2026-12-01T08:00:00Z"), price: 500 });

    expect(result.errors).toEqual({});
  });

  it("accepts an entry taking effect now without validFrom", async () => {
    const result = await resolve({ type: "now", validFrom: null, price: 500 });

    expect(result.errors).toEqual({});
  });
});
