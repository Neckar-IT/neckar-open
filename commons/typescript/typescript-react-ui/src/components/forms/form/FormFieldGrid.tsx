import { cn } from "cn";

/**
 * A group of related form fields laid out side by side, two per row — the **single source of the
 * form-field grid layout**, independent of where the group lives.
 *
 * Use it for a fixed set of fields that belong to one value object and read better next to each
 * other than stacked (the four margins of an inset, a width/height pair). [FormBody] stays the
 * default: it stacks fields vertically and is what a form's fields sit in unless they form such a
 * group. [FieldArrayRowFields] composes this grid for the fields of a `useFieldArray` row.
 *
 * Pass `className` to widen the grid where a group has more than four fields and the screen allows
 * it (e.g. `md:grid-cols-3`). Never write the grid classes inline.
 */
export function FormFieldGrid({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  return <div className={cn("grid grid-cols-2 gap-3", className)} {...props} />;
}
