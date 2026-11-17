import { Form } from "@neckar-hub/typescript-react-ui/forms";
import { Card, CardContent, CardFooter } from "@neckar-hub/typescript-react-ui/layout";
import { cn } from "cn";
import type { FieldValues, UseFormReturn } from "react-hook-form";
import { FormBody } from "./FormBody";
import { FormRootErrorMessage } from "./FormRootErrorMessage";

interface FormCardProps<T extends FieldValues> {
  form: UseFormReturn<T>;
  onSubmit?: (e?: React.BaseSyntheticEvent) => void | Promise<void>;
  children: React.ReactNode;
}

/**
 * Card with an embedded Form. Wraps children in `<Card><Form>...</Form></Card>`.
 */
export function FormCard<T extends FieldValues>({ form, onSubmit, children }: FormCardProps<T>): React.ReactElement {
  return (
    <Form {...form} onSubmit={onSubmit}>
      <Card>{children}</Card>
    </Form>
  );
}

/**
 * CardContent with default form layout (vertical stack with gap and top padding).
 *
 * Sets `text-sm` so inline read-only values (e.g. a {@link BookLink} shown in update mode
 * for an immutable field) match the visual rhythm of the surrounding Labels and Inputs,
 * which are `text-sm` by convention. Form fields that wrap their own Input keep their
 * existing styling — this only affects bare children rendered directly into the card.
 */
export function FormCardContent({ children, className }: { children: React.ReactNode; className?: string }): React.ReactElement {
  return (
    <CardContent>
      <FormBody className={className}>{children}</FormBody>
    </CardContent>
  );
}

/**
 * CardFooter with default form layout (vertical stack with gap and top padding).
 *
 * Renders {@link FormRootErrorMessage} above its children, so the form's root error stands above the `ButtonBar`.
 *
 * Explicitly sets `items-stretch` to neutralize `items-center` inherited from
 * `CardFooter` — without it, `items-center` turns into horizontal centering
 * under `flex-col`, which centers children like `ButtonBar` instead of
 * letting their internal `justify-end` take effect.
 */
export function FormCardFooter({ children, className }: { children: React.ReactNode; className?: string }): React.ReactElement {
  return (
    <CardFooter className={cn("flex flex-col items-stretch gap-4", className)}>
      <FormRootErrorMessage />
      {children}
    </CardFooter>
  );
}
