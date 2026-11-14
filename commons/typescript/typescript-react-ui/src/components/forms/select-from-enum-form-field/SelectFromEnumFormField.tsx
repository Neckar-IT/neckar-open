/**
 * Imports
 */

import { extractEnumFromFormSchema } from "@neckar-hub/typescript-utils/zod-utils";
import type { ZodObject } from "zod";
import { SelectFromEnum } from "../../inputs/select-from-enum/SelectFromEnum";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

interface SelectFromEnumFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  placeholder?: string;
  /**
   * Name of the enum behind this field, e.g. `"MonitoringSystem"` — qualifies the label lookup.
   * See {@link SelectFromEnum}.
   */
  enumName?: string;
}

/**
 * Select Form Field explicitly for Enums
 */

export function SelectFromEnumFormField<T extends ZodObject>({ enumName, ...rest }: SelectFromEnumFormFieldProps<T>): React.ReactElement {
  const extractedEnum = extractEnumFromFormSchema(rest.formSchema, rest.fieldName);

  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <SelectFromEnum {...field} isInvalid={fieldState.invalid} enumObject={extractedEnum} enumName={enumName} />
      )}
    />
  );
}
