/**
 * Imports
 */

import { extractEnumFromFormSchema } from "@neckar-hub/typescript-utils/zod-utils";
import type { VariantProps } from "class-variance-authority";
import { cn } from "cn";
import type { ZodObject } from "zod";
import { ToggleGroupFromEnum } from "../../inputs/toggle-group-from-enum/ToggleGroupFromEnum";
import type { toggleVariants } from "../../primitives/toggle";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

interface ToggleGroupFromEnumFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  variant?: VariantProps<typeof toggleVariants>["variant"];
  /**
   * Name of the enum behind this field, e.g. `"MonitoringSystem"` — qualifies the label lookup.
   * See {@link ToggleGroupFromEnum}.
   */
  enumName?: string;
}

/**
 * ToggleGroup Form Field explicitly for Enums
 */

export function ToggleGroupFromEnumFormField<T extends ZodObject>({
  className,
  variant = "outline",
  enumName,
  ...rest
}: ToggleGroupFromEnumFormFieldProps<T>): React.ReactElement {
  const extractedEnum = extractEnumFromFormSchema(rest.formSchema, rest.fieldName);

  return (
    <FormFieldBase
      {...rest}
      className={cn("gap-2", className)}
      render={({ field: { onChange, value, ...fieldRest } }) => (
        <ToggleGroupFromEnum
          variant={variant}
          className="justify-start"
          value={value as string}
          onValueChange={(newValue) => {
            // Prevent deselection — always keep one item selected
            if (newValue) {
              onChange(newValue);
            }
          }}
          enumObject={extractedEnum}
          enumName={enumName}
          {...fieldRest}
        />
      )}
    />
  );
}
