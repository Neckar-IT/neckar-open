import type { PropsWithChildren } from "react";
import { useFormContext, useFormState } from "react-hook-form";
import type { ZodObject } from "zod";
import { FormField, type FormFieldBaseProps, FormItem } from "../form";
import { CheckboxDependentFields } from "./CheckboxDependentFields";
import { CheckboxRow } from "./CheckboxRow";

interface OptionalValueCheckboxFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T>, PropsWithChildren {
  /**
   * The value the field is set to when the box is checked. Its properties are the ones a fresh
   * value starts in; a property with no such state is left out, so the schema demands it as soon
   * as the value is there.
   */
  presentValue: object;
}

/**
 * Checkbox bound to a form field holding a value or nothing: checked writes {@link presentValue},
 * unchecked writes `null`, and the fields of the value are mounted only while it is there.
 *
 * The box is no field of its own — what it reads and writes is the value itself — so the form
 * state stays what the request carries.
 */
export function OptionalValueCheckboxFormField<T extends ZodObject>({
  presentValue,
  children,
  ...props
}: OptionalValueCheckboxFormFieldProps<T>): React.ReactElement {
  const { control } = useFormContext();
  const { isSubmitting, disabled } = useFormState();

  const disabledOrSubmitting = props.disabled || isSubmitting || disabled;

  return (
    <FormField
      control={control}
      name={props.fieldName}
      render={({ field }) => {
        const present = field.value !== null && field.value !== undefined;

        return (
          <FormItem className={props.className}>
            <CheckboxRow
              checked={present}
              description={props.description}
              descriptionComponent={props.descriptionComponent}
              disabled={disabledOrSubmitting}
              label={props.label}
              labelComponent={props.labelComponent}
              name={field.name}
              onBlur={field.onBlur}
              onCheckedChange={(checked) => {
                field.onChange(checked ? presentValue : null);
              }}
            />
            {present && <CheckboxDependentFields>{children}</CheckboxDependentFields>}
          </FormItem>
        );
      }}
    />
  );
}
