import { describe, expect, it } from "vitest";
import { emptyDraftAddress, toConfirmedAddress } from "./addressDraftConfirmed";

describe("emptyDraftAddress", () => {
  it("is a Draft without any field but the given country", () => {
    expect(emptyDraftAddress("Austria")).toEqual({
      type: "Draft",
      street: null,
      houseNumber: null,
      zipCode: null,
      city: null,
      country: "Austria",
    });
  });
});

describe("toConfirmedAddress", () => {
  const complete = {
    type: "Draft" as const,
    street: "Neckarstraße",
    houseNumber: "12",
    zipCode: "72072",
    city: "Tübingen",
    country: null,
  };

  it("confirms a complete Draft, with the default country for a Draft without one", () => {
    expect(toConfirmedAddress(complete, "Germany")).toEqual({ ...complete, type: "Confirmed", country: "Germany" });
    expect(toConfirmedAddress({ ...complete, country: "Austria" }, "Germany")?.country).toBe("Austria");
  });

  it("answers null for a Draft without a required field", () => {
    expect(toConfirmedAddress({ ...complete, houseNumber: null }, "Germany")).toBeNull();
    expect(toConfirmedAddress({ ...complete, city: "" }, "Germany")).toBeNull();
  });
});
