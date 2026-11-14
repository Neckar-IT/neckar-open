import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import get from "lodash/get";
import type { FieldError, FieldPath } from "react-hook-form";
import { useFormContext } from "react-hook-form";
import type { z, ZodObject } from "zod";
import { FormErrorText } from "./FormErrorText";

/**
 * The error of an array field carries the list-level message under `root` once the array is
 * registered through `useFieldArray`, and on the node itself otherwise.
 */
type ArrayFieldError = FieldError & { root?: FieldError };

export interface FormArrayErrorMessageProps<T extends ZodObject> {
  /**
   * The name of the array field in the form schema, e.g. `"ingredients"`.
   */
  fieldName: FieldPath<z.infer<T>>;
  /**
   * The form schema the array field belongs to.
   */
  formSchema: T;
  className?: string;
}

/**
 * The validation error of an array field itself — the message a schema like `z.array(...).min(1)`
 * produces for the whole list, as opposed to the errors of its rows.
 *
 * react-hook-form keeps that error apart from the row errors, so no form field owns it: the row
 * fields render the errors of `ingredients.0.name`, and the list-level message would never be
 * shown. Render this once next to the rows of a `useFieldArray`.
 */
export function FormArrayErrorMessage<T extends ZodObject>({
  fieldName,
  className,
}: FormArrayErrorMessageProps<T>): React.ReactElement | null {
  const { formState } = useFormContext();

  const errorForArray = get(formState.errors, fieldName) as ArrayFieldError | undefined;
  const message = errorForArray?.root?.message ?? errorForArray?.message;

  if (isNotDefined(message)) {
    return null;
  }

  return (
    <FormErrorText className={className} role="alert">
      {message}
    </FormErrorText>
  );
}
