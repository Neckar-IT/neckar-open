/**
 * Imports
 */

import { type CountryCode, parsePhoneNumberFromString } from "libphonenumber-js/max";

/**
 * Formats a phone number
 *
 * @param value - The string that contains the phone number
 * @param country - The country code of the user (defaults to "DE")
 */

export function formatPhoneNumber(value: string, country: CountryCode = "DE"): { isValid: boolean; value: string } {
  const phoneNumber = parsePhoneNumberFromString(value, country);

  if (phoneNumber?.isValid()) {
    return {
      isValid: true,
      value: phoneNumber.country === country ? phoneNumber.formatNational() : phoneNumber.formatInternational(),
    };
  }

  return {
    isValid: false,
    value,
  };
}
