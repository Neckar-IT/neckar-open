/**
 * Imports
 */

import { cn } from "cn";
import { useFormContext, useFormState } from "react-hook-form";
import { type ZodObject } from "zod";
import { Switch } from "../../primitives/switch";
import { FormControl, FormField, FormFieldAnnotation, type FormFieldBaseProps, FormFieldLabel, FormItem } from "../form";

/**
 * React-hook-form field wrapper around Switch for boolean toggle inputs with label and description.
 */

export function SwitchFormField<T extends ZodObject>(props: FormFieldBaseProps<T>): React.ReactElement {
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
            <div className="flex items-start gap-2">
              <FormControl>
                <Switch
                  checked={field.value ?? undefined}
                  disabled={disabledOrSubmitting}
                  name={field.name}
                  onBlur={field.onBlur}
                  onCheckedChange={handleChange}
                />
              </FormControl>
              <div className={cn({ "mt-1": !props.description })}>
                <FormFieldLabel disabled={disabledOrSubmitting} label={props.label} labelComponent={props.labelComponent} />

                <FormFieldAnnotation
                  disabled={disabledOrSubmitting}
                  description={props.description}
                  descriptionComponent={props.descriptionComponent}
                />
              </div>
            </div>
          </FormItem>
        );
      }}
    />
  );
}
