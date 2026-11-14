/**
 * Imports
 */

import { cn } from "cn";
import { z, type ZodObject } from "zod";
import { TagPicker, type TagPickerProps } from "../../inputs/tag-picker/TagPicker";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface TagPickerFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  options: TagPickerProps["options"];
  textKeys?: TagPickerProps["textKeys"];
}

/**
 * Zod Schema to use in formSchemas for the TagPickerFormField
 */

export const tagsSchema = z.array(
  z.object({
    id: z.string(),
    name: z.string(),
  }),
);

/**
 * React-hook-form field wrapper around TagPicker for selecting multiple tags.
 */

export function TagPickerFormField<T extends ZodObject>({ options, textKeys, ...rest }: TagPickerFormFieldProps<T>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <TagPicker
          {...field}
          options={options}
          textKeys={textKeys}
          className={cn("w-full", { "border-destructive focus-visible:ring-destructive/30": fieldState.invalid })}
        />
      )}
    />
  );
}
