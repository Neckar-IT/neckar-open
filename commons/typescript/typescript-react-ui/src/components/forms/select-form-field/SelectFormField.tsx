/**
 * Imports
 */

import type { PropsWithChildren, ReactElement } from "react";
import type { ZodObject } from "zod";
import { Select } from "../../inputs/select";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Type Declarations
 */

export type SelectFormFieldProps<T extends ZodObject> = PropsWithChildren<
  FormFieldBaseProps<T> & {
    placeholder?: string;
    hideClearButton?: boolean;
  }
>;

/**
 * Select dropdown bound to a react-hook-form field.
 */

export function SelectFormField<T extends ZodObject>({
  children,
  placeholder,
  hideClearButton,
  ...rest
}: SelectFormFieldProps<T>): ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <Select placeholder={placeholder} isInvalid={fieldState.invalid} hideClearButton={hideClearButton} {...field}>
          {children}
        </Select>
      )}
    />
  );
}
