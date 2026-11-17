/**
 * Imports
 */

import { type CountryCode, formatPhoneNumber } from "@neckar-hub/typescript-react-utils/formatters";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";

/**
 * Prop Types
 */

interface PhoneNumberProps {
  value: string | null | undefined;
  countryCode?: CountryCode;
  className?: string;
}

/**
 * Component to display a formatted phone number
 *
 * @prop value - The phone number to format and display
 * @prop country - The country code for formatting (defaults to "DE")
 * @prop className - Additional CSS classes for styling
 * @prop placeholder - Placeholder text if the phone number is null or undefined. Returns null if not provided.
 */

export function PhoneNumber({ value, countryCode = "DE", className }: PhoneNumberProps): React.ReactNode {
  if (!isDefined(value)) {
    return null;
  }

  const { value: formattedPhoneNumber } = formatPhoneNumber(value, countryCode);
  return <span className={cn("whitespace-nowrap", className)}>{formattedPhoneNumber}</span>;
}
