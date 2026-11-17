import { cn } from "cn";
import type { PropsWithChildren } from "react";
import { FormBody } from "../form";

interface CheckboxDependentFieldsProps extends PropsWithChildren {
  className?: string;
}

/**
 * Layout for the form fields a {@link CheckboxFormField} gates: the same vertical rhythm as the
 * rest of the form, indented to the checkbox's label so the fields read as belonging to it.
 *
 * The indent matches the checkbox layout in `CheckboxFormField` — a `h-4 w-4` box plus the `gap-2`
 * to its label — which is why this component lives next to it: the two are changed together.
 *
 * Renders nothing about visibility; the caller decides whether the group is mounted at all, since
 * an unchecked box means the values are not sent rather than merely hidden.
 */
export function CheckboxDependentFields({ children, className }: CheckboxDependentFieldsProps): React.ReactElement {
  return <FormBody className={cn("pl-6", className)}>{children}</FormBody>;
}
