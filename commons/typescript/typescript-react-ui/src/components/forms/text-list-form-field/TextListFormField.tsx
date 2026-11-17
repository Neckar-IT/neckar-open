/**
 * Imports
 */

import type { ZodObject } from "zod";
import { TextList, type TextListProps } from "../../inputs/text-list";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface TextListFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T, string[]> {
  maxEntries?: TextListProps["maxEntries"];
  maxLength?: TextListProps["maxLength"];
  placeholder?: TextListProps["placeholder"];
  textKeys?: TextListProps["textKeys"];
}

/**
 * React-hook-form field wrapper around TextList for a list of free texts.
 */

export function TextListFormField<T extends ZodObject>({
  maxEntries,
  maxLength,
  placeholder,
  textKeys,
  ...rest
}: TextListFormFieldProps<T>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field }) => (
        <TextList
          value={field.value as string[] | null | undefined}
          onChange={field.onChange}
          disabled={field.disabled}
          maxEntries={maxEntries}
          maxLength={maxLength}
          placeholder={placeholder}
          textKeys={textKeys}
        />
      )}
    />
  );
}
