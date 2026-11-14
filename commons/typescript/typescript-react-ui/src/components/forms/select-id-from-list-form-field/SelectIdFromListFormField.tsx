/**
 * Imports
 */

import { type ZodObject } from "zod";
import { SelectIdFromList } from "../../inputs/select-id-from-list";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface SelectFromListFormFieldProps<T extends ZodObject, U extends { id: string }> extends FormFieldBaseProps<T> {
  options: U[];
  itemRenderer: (item: U) => React.ReactNode;
  placeholder?: string;
  hideClearButton?: boolean;
}

/**
 * React-hook-form field wrapper around SelectIdFromList for selecting an item's id from a typed list.
 */

export function SelectIdFromListFormField<T extends ZodObject, U extends { id: string }>({
  options,
  itemRenderer,
  placeholder,
  hideClearButton,
  ...rest
}: SelectFromListFormFieldProps<T, U>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <SelectIdFromList
          {...field}
          placeholder={placeholder}
          isInvalid={fieldState.invalid}
          options={options}
          itemRenderer={itemRenderer}
          hideClearButton={hideClearButton}
        />
      )}
    />
  );
}
