/**
 * Imports
 */

import { type CountryCode, formatPhoneNumber } from "@neckar-hub/typescript-react-utils/formatters";
import type { ChangeEventHandler } from "react";
import { InputWrapper } from "../input-wrapper";

/**
 * Type Declarations for the Phone Number Input Component
 */

export interface PhoneNumberInputProps extends Omit<React.ComponentProps<"input">, "value" | "onChange" | "type"> {
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  countryCode?: CountryCode | undefined;
  isInvalid?: boolean;
}

/**
 * Phone number input that formats the value on blur according to the given country code.
 *
 * @prop countryCode - the country code of the user (defaults to "DE")
 */

export function PhoneNumberInput({ value, onChange, onBlur, countryCode, isInvalid, className, ...rest }: PhoneNumberInputProps) {
  const sanitizedValue = value ?? "";
  const handleChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    const value = event.target.value;
    if (value === "") {
      onChange(null);
    } else {
      onChange(value);
    }
  };

  // format the phone number when the input looses focus; an empty input stays `null`
  const handleBlur = (event: React.FocusEvent<HTMLInputElement>) => {
    if (event.target.value.trim() === "") {
      onChange(null);
    } else {
      const phoneNumber = formatPhoneNumber(event.target.value, countryCode);
      onChange(phoneNumber.value);
    }
    if (onBlur) {
      onBlur(event);
    }
  };

  const handleClear = () => {
    onChange(null);
  };

  return (
    <InputWrapper disabled={rest.disabled} hideClearButton={!value} onClear={handleClear} isInvalid={isInvalid} className={className}>
      <input
        type="tel"
        value={sanitizedValue}
        onBlur={handleBlur}
        onChange={handleChange}
        className="placeholder:text-muted-foreground flex-1 px-3 py-2 text-sm focus-visible:outline-hidden disabled:cursor-not-allowed"
        {...rest}
      />
    </InputWrapper>
  );
}
