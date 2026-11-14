/**
 * Imports
 */

import { type ZodObject } from "zod";
import { SelectFromList } from "../../inputs/select-from-list";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface SelectFromListFormFieldProps<T extends ZodObject, U> extends FormFieldBaseProps<T> {
  options: U[];
  itemRenderer: (item: U) => React.ReactNode;
  placeholder?: string;
}

/**
 * React-hook-form field wrapper around SelectFromList for selecting a typed object from a list.
 */

export function SelectFromListFormField<T extends ZodObject, U>({
  options,
  itemRenderer,
  placeholder,
  ...rest
}: SelectFromListFormFieldProps<T, U>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <SelectFromList {...field} placeholder={placeholder} isInvalid={fieldState.invalid} options={options} itemRenderer={itemRenderer} />
      )}
    />
  );
}
