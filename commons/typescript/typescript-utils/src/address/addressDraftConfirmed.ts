import type { Address, AddressConfirmedWithType } from "../openapi-types";

/**
 * Empty Draft address that seeds the edit form when no address exists yet; `country` pre-fills the
 * country field.
 */
export function emptyDraftAddress(country: string | null): Address {
  return {
    type: "Draft",
    street: null,
    houseNumber: null,
    zipCode: null,
    city: null,
    country,
  };
}

/**
 * `null` while street, house number, zip code or city is missing; a Draft without a country takes `defaultCountry`.
 */
export function toConfirmedAddress(address: Address, defaultCountry: string): AddressConfirmedWithType | null {
  const { street, houseNumber, zipCode, city } = address;
  if (!street || !houseNumber || !zipCode || !city) {
    return null;
  }
  return {
    type: "Confirmed",
    street,
    houseNumber,
    zipCode,
    city,
    country: address.country ?? defaultCountry,
  };
}
