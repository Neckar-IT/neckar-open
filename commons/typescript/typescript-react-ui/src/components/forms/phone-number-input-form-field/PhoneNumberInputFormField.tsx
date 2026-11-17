/**
 * Imports
 */

import type { CountryCode } from "@neckar-hub/typescript-react-utils/formatters";
import { type ZodObject } from "zod";
import { PhoneNumberInput } from "../../inputs/phone-number-input";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface PhoneNumberInputFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  countryCode?: CountryCode | undefined;
}

/**
 * Phone number input with country-code formatting, bound to a react-hook-form field.
 */

export function PhoneNumberInputFormField<T extends ZodObject>({ countryCode, ...rest }: PhoneNumberInputFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => <PhoneNumberInput {...field} countryCode={countryCode} isInvalid={fieldState.invalid} />}
    />
  );
}
