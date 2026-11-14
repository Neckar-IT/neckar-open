import { cn } from "cn";

/**
 * The body of a form: stacks the form fields vertically with the standard spacing (`gap-4`,
 * `text-sm`) — the **single source of the form-field stack layout**, independent of where the form
 * lives.
 *
 * Wrap it in the matching container chrome instead of using it directly:
 * [FormCardContent] (page forms in a card), [FormDialogBody] (dialog forms) and [FormFieldset]
 * (a group inside a form) compose it. Use
 * `FormBody` directly only when a form has neither container (e.g. stories). Never write the stack
 * classes inline — a form container that misses them renders its fields without any gap (that is
 * how the first form dialogs diverged from page forms, see issue #2431).
 */
export function FormBody({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  return <div className={cn("flex flex-col gap-4 text-sm", className)} {...props} />;
}
