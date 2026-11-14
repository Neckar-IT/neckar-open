/**
 * Imports
 */

import { isFieldRequiredInFormSchema } from "@neckar-hub/typescript-utils/zod-utils";
import { cn } from "cn";
import { useFormContext } from "react-hook-form";
import { type ZodObject } from "zod";
import { ImageUpload, ImageUploadInput, ImageUploadPreview, type ImageUploadProps } from "../../media/image-upload";
import { FormControl, FormField, FormFieldAnnotation, FormFieldLabel, FormItem, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface ImageUploadFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  preview?: string | null | undefined;
  previewClassName?: string;
  accept?: ImageUploadProps["accept"];
}

/**
 * File upload input with image preview, bound to a react-hook-form field.
 *
 * @prop preview - An image to be shown while no file is selected (e.g. the previous image)
 */

export function ImageUploadFormField<T extends ZodObject>({
  accept = "image/*",
  preview,
  previewClassName,
  disabled,
  label,
  labelComponent,
  description,
  descriptionComponent,
  className,
  fieldName,
  formSchema,
}: ImageUploadFormFieldProps<T>): React.ReactElement {
  const { control, trigger } = useFormContext();

  return (
    <FormField
      control={control}
      name={fieldName}
      render={({ field, fieldState, formState }) => {
        const disabledOrSubmitting = disabled || formState.isSubmitting;
        const required = isFieldRequiredInFormSchema(formSchema, fieldName);

        const handleChange = (value: File | undefined) => {
          field.onChange(value);
          if (fieldState.error) {
            void trigger(fieldName);
          }
        };

        return (
          <FormItem className={className}>
            <FormFieldLabel disabled={disabledOrSubmitting} required={required} label={label} labelComponent={labelComponent} />

            <FormControl>
              <ImageUpload
                accept={accept}
                className="flex items-start gap-2"
                disabled={disabledOrSubmitting}
                value={field.value}
                onChange={handleChange}
              >
                <ImageUploadPreview
                  className={cn("aspect-video w-32 rounded-md border object-contain", previewClassName)}
                  preview={preview}
                />
                <div className="flex-1">
                  <ImageUploadInput className={cn({ "border-destructive focus-visible:ring-destructive/30": fieldState.error })} />
                  <FormFieldAnnotation
                    disabled={disabledOrSubmitting}
                    description={description}
                    descriptionComponent={descriptionComponent}
                  />
                </div>
              </ImageUpload>
            </FormControl>
          </FormItem>
        );
      }}
    />
  );
}
