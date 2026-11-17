import { cn } from "cn";

/**
 * The validation-error text of a form — the single source of the error-text styling.
 *
 * Composed by [FormMessage] (the error of one form field, read from the field context), by
 * [FormArrayErrorMessage] (the list-level error of an array field, which belongs to no field) and by
 * [FormRootErrorMessage] (the error under `formState.errors.root`, which belongs to the whole form).
 */
export function FormErrorText({ className, ...props }: React.ComponentProps<"p">): React.ReactElement {
  return <p className={cn("text-destructive text-sm font-medium", className)} {...props} />;
}
