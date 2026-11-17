import { isNotDefined } from "../lang";
import type { AddressConfirmed, AddressDraft } from "../openapi-types";

/**
 * Grouped utility functions for formatting an address
 */
export class Address {
  /**
   * Combines the street and house number into a string, returns undefined if both are undefined
   */
  static toStreetAndHouseNumberString(address: AddressConfirmed | AddressDraft | null | undefined): string | undefined {
    if (isNotDefined(address)) {
      return undefined;
    }

    if (isNotDefined(address.street) && isNotDefined(address.houseNumber)) {
      return undefined;
    }

    return [address.street, address.houseNumber].filter(Boolean).join(" ");
  }

  /**
   * Combines the zipCode and city into a string, returns undefined if both are undefined
   */
  static toZipCodeAndCityString(address: AddressConfirmed | AddressDraft | null | undefined): string | undefined {
    if (isNotDefined(address)) {
      return undefined;
    }

    if (isNotDefined(address.zipCode) && isNotDefined(address.city)) {
      return undefined;
    }

    return [address.zipCode, address.city].filter(Boolean).join(" ");
  }

  /**
   * Combines the entire address into a string, returns undefined if no properties are defined
   */
  static toString(address: AddressConfirmed | AddressDraft | null | undefined): string | undefined {
    const streetPart = this.toStreetAndHouseNumberString(address);
    const cityPart = this.toZipCodeAndCityString(address);

    if (isNotDefined(streetPart) && isNotDefined(cityPart)) {
      return undefined;
    }

    return [streetPart, cityPart].filter(Boolean).join(", ");
  }
}
