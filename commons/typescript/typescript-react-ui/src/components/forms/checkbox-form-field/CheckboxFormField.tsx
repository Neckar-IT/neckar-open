import { useFormContext, useFormState } from "react-hook-form";
import type { ZodObject } from "zod";
import { FormField, type FormFieldBaseProps, FormItem } from "../form";
import { CheckboxRow } from "./CheckboxRow";

/**
 * Checkbox bound to a react-hook-form field.
 */
export function CheckboxFormField<T extends ZodObject>(props: FormFieldBaseProps<T>): React.ReactElement {
  const { control, trigger } = useFormContext();
  const { isSubmitting } = useFormState();

  const disabledOrSubmitting = props.disabled || isSubmitting;

  return (
    <FormField
      control={control}
      name={props.fieldName}
      render={({ field, fieldState }) => {
        const handleChange = (value: boolean) => {
          field.onChange(value);
          if (fieldState.error) {
            void trigger(props.fieldName);
          }
        };

        return (
          <FormItem className={props.className}>
            <CheckboxRow
              checked={field.value ?? undefined}
              description={props.description}
              descriptionComponent={props.descriptionComponent}
              disabled={disabledOrSubmitting}
              label={props.label}
              labelComponent={props.labelComponent}
              name={field.name}
              onBlur={field.onBlur}
              onCheckedChange={handleChange}
            />
          </FormItem>
        );
      }}
    />
  );
}
