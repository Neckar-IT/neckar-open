/**
 * Imports
 */

import { isFieldRequiredInFormSchema } from "@neckar-hub/typescript-utils/zod-utils";
import { type ControllerProps, type FieldPathByValue, useFormContext } from "react-hook-form";
import type { z, ZodObject } from "zod";
import { FormControl, FormField, FormItem } from "./Form";
import { FormFieldAnnotation, type FormFieldAnnotationProps } from "./FormFieldAnnotation";
import { FormFieldLabel, type FormFieldLabelProps } from "./FormFieldLabel";

/**
 * Props for the base form field component.
 *
 * `TFieldValue` narrows which fields of the schema a concrete field component accepts. A
 * wrapper whose input reads and writes a specific form-state type declares that type, and
 * `fieldName` then only accepts schema fields carrying it — e.g. `DatePickerFormField`
 * (ISO `yyyy-MM-dd` string) rejects a `z.date()` field at compile time instead of throwing
 * during render. The default `unknown` accepts every field, so wrappers that work with any
 * value type stay unconstrained.
 */
export interface FormFieldBaseProps<T extends ZodObject, TFieldValue = unknown> {
  /**
   * The name of the field. This is used to reference the form values in the form schema.
   */
  fieldName: FieldPathByValue<z.infer<T>, TFieldValue>;
  /**
   * The form schema that is used to fetch the values and determine other properties like "required"
   */
  formSchema: T;
  /**
   * The label for the form field - that will be translated
   */
  label?: FormFieldLabelProps["label"];
  /**
   * Custom label component to use instead of the default label
   */
  labelComponent?: FormFieldLabelProps["labelComponent"];

  /**
   * The description for the form field - that will be translated
   */
  description?: FormFieldAnnotationProps["description"];
  /**
   * Custom description component to use instead of the default description
   */
  descriptionComponent?: FormFieldAnnotationProps["descriptionComponent"];

  /**
   * If true, a blue asterisk will be displayed next to the label.
   * Indicates that this field is required for confirmation (but optional for saving as draft).
   */
  softRequired?: boolean;

  /**
   * If true, the input will be disabled
   */
  disabled?: boolean;

  className?: string;
}

/**
 * This is a wrapper component for inputs to enable the inputs to be used inside a form with react-hook-form
 *
 * ## Purpose:
 *  - Unified rendering of label, required indicator, description, and error message
 *  - Automatically connects the input to react-hook-form (Controller-style)
 *  - Automatically disables input while submitting, and while the whole form is disabled
 *    via `useForm({ disabled })` (form-level read-only, e.g. an edit-blocked entity)
 *  - Automatically re-triggers validation after correction
 *
 * ## Requirements
 *  - Must be rendered inside a `<FormProvider/>` (react-hook-form).
 *
 * ### HTML `required`
 *  - the `isRequired` state of the field is derived from the provided Zod schema (`formSchema`) using `isFieldRequiredInFormSchema(...)`.
 */
export function FormFieldBase<T extends ZodObject>(
  props: FormFieldBaseProps<T> & {
    render: ControllerProps["render"];
  },
): React.ReactElement {
  const { control, trigger, formState } = useFormContext();

  const disabledOrSubmitting = props.disabled || formState.isSubmitting || formState.disabled;
  const isRequired = isFieldRequiredInFormSchema(props.formSchema, props.fieldName);

  return (
    <FormField
      control={control}
      name={props.fieldName}
      render={({ field, fieldState, formState }) => {
        const handleChange = (value: unknown) => {
          field.onChange(value);
          if (fieldState.error) {
            //trigger validation if this field had an error before
            void trigger(props.fieldName);
          }
        };

        return (
          <FormItem className={props.className}>
            <FormFieldLabel
              label={props.label}
              labelComponent={props.labelComponent}
              disabled={disabledOrSubmitting}
              required={isRequired}
              softRequired={props.softRequired}
            />

            {/* The wrapping is what ties the control to its label, description and error
                message — see [FormControl] for what the render result has to be. */}
            <FormControl>
              {props.render({
                field: { ...field, onChange: handleChange, disabled: disabledOrSubmitting },
                fieldState,
                formState,
              })}
            </FormControl>

            <FormFieldAnnotation
              disabled={disabledOrSubmitting}
              description={props.description}
              descriptionComponent={props.descriptionComponent}
            />
          </FormItem>
        );
      }}
    />
  );
}
