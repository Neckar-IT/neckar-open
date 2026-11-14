import { describe, expect, it } from "vitest";
import type { AddressDraft } from "../openapi-types";
import { Address } from "./Address";

function draftAddress(partial: Partial<AddressDraft>): AddressDraft {
  return { street: null, houseNumber: null, zipCode: null, city: null, country: null, ...partial };
}

describe("Address.toStreetAndHouseNumberString", () => {
  it("should return undefined if street and houseNumber are null", () => {
    expect(Address.toStreetAndHouseNumberString(draftAddress({ city: "Musterstadt" }))).toBe(undefined);
  });

  it("should return the street and houseNumber", () => {
    expect(Address.toStreetAndHouseNumberString(draftAddress({ street: "Musterstr." }))).toBe("Musterstr.");
    expect(Address.toStreetAndHouseNumberString(draftAddress({ houseNumber: "12" }))).toBe("12");
    expect(Address.toStreetAndHouseNumberString(draftAddress({ street: "Musterstr.", houseNumber: "12" }))).toBe("Musterstr. 12");
  });
});

describe("Address.toZipCodeAndCityString", () => {
  it("should return undefined if zipCode and city are null", () => {
    expect(Address.toZipCodeAndCityString(draftAddress({ street: "Musterstr." }))).toBe(undefined);
  });

  it("should return the zipCode and city", () => {
    expect(Address.toZipCodeAndCityString(draftAddress({ zipCode: "12345" }))).toBe("12345");
    expect(Address.toZipCodeAndCityString(draftAddress({ city: "Musterstadt" }))).toBe("Musterstadt");
    expect(Address.toZipCodeAndCityString(draftAddress({ zipCode: "12345", city: "Musterstadt" }))).toBe("12345 Musterstadt");
  });
});

describe("Address.toString", () => {
  it("should return undefined if no address parts are set", () => {
    expect(Address.toString(draftAddress({}))).toBe(undefined);
  });

  it("should return the full address string", () => {
    expect(Address.toString(draftAddress({ street: "Musterstr.", houseNumber: "12", zipCode: "12345", city: "Musterstadt" }))).toBe(
      "Musterstr. 12, 12345 Musterstadt",
    );
    expect(Address.toString(draftAddress({ street: "Musterstr.", houseNumber: "12" }))).toBe("Musterstr. 12");
    expect(Address.toString(draftAddress({ zipCode: "12345", city: "Musterstadt" }))).toBe("12345 Musterstadt");
  });
});
