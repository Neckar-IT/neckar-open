/**
 * Imports
 */

import { enumTranslationKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import { extractEnumFromArrayFormSchema } from "@neckar-hub/typescript-utils/zod-utils";
import { cn } from "cn";
import { useId } from "react";
import type { ZodObject } from "zod";
import { Checkbox } from "../../primitives/checkbox";
import { FormFieldBase, type FormFieldBaseProps, hasFormFieldLabel, useFormField } from "../form";
import { FormLabel } from "../form/Form";

/**
 * Prop Types
 */

interface CheckboxGroupFromEnumFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  /**
   * Name of the enum behind the array elements, e.g. `"ContentWarning"` — qualifies the label lookup.
   * See {@link enumTranslationKeys}.
   */
  enumName?: string;
}

/**
 * Multi-select of an enum: one checkbox per enum value, bound to an array field.
 * The value holds the checked values in enum order.
 */

export function CheckboxGroupFromEnumFormField<T extends ZodObject>({
  className,
  enumName,
  ...rest
}: CheckboxGroupFromEnumFormFieldProps<T>): React.ReactElement {
  const enumValues = Object.values(extractEnumFromArrayFormSchema(rest.formSchema, rest.fieldName));

  return (
    <FormFieldBase
      {...rest}
      className={cn("gap-2", className)}
      render={({ field: { onChange, value, disabled, name, onBlur } }) => {
        const checkedValues: readonly (string | number)[] = Array.isArray(value) ? value : [];
        const toggle = (toggled: string | number, checked: boolean) =>
          onChange(enumValues.filter((enumValue) => (enumValue === toggled ? checked : checkedValues.includes(enumValue))));

        return (
          <CheckboxGroup labelled={hasFormFieldLabel(rest)}>
            {enumValues.map((enumValue) => (
              <CheckboxGroupFromEnumItem
                key={enumValue}
                value={enumValue}
                enumName={enumName}
                name={name}
                checked={checkedValues.includes(enumValue)}
                disabled={disabled}
                onBlur={onBlur}
                onCheckedChange={(checked) => toggle(enumValue, checked)}
              />
            ))}
          </CheckboxGroup>
        );
      }}
    />
  );
}

/**
 * The group the field label names; takes the id and aria attributes `FormControl` merges into it
 */

function CheckboxGroup({ labelled, children, ...controlProps }: React.ComponentProps<"div"> & { labelled: boolean }): React.ReactElement {
  const { formLabelId } = useFormField();

  return (
    <div
      role="group"
      aria-labelledby={labelled ? formLabelId : undefined}
      className="grid grid-cols-1 gap-2 pl-2 md:grid-cols-2"
      {...controlProps}
    >
      {children}
    </div>
  );
}

/**
 * One checkbox with the translated enum value beside it
 */

function CheckboxGroupFromEnumItem({
  value,
  enumName,
  name,
  checked,
  disabled,
  onBlur,
  onCheckedChange,
}: {
  value: string | number;
  enumName?: string;
  name: string;
  checked: boolean;
  disabled?: boolean;
  onBlur: () => void;
  onCheckedChange: (checked: boolean) => void;
}): React.ReactElement {
  const { t } = useTranslation();
  const id = useId();

  return (
    <div className="flex items-center gap-2">
      <Checkbox
        id={id}
        name={name}
        value={value.toString()}
        checked={checked}
        disabled={disabled}
        onBlur={onBlur}
        onCheckedChange={(state) => onCheckedChange(state === true)}
      />
      <FormLabel htmlFor={id} className="font-normal">
        {t(enumTranslationKeys(value, enumName))}
      </FormLabel>
    </div>
  );
}
