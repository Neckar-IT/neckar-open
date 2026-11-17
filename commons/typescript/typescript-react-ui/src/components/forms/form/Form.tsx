/**
 * shadcn Form Component (https://ui.shadcn.com/docs/components/form)
 */

/**
 * Imports
 */

import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import { type Label as LabelPrimitive, Slot } from "radix-ui";
import { createContext, useContext, useId, useMemo } from "react";
import type { ControllerProps, FieldPath, FieldValues, FormProviderProps } from "react-hook-form";
import { Controller, FormProvider, useFormContext } from "react-hook-form";
import { Label } from "../../primitives/label";
import { FormDebug } from "./FormDebug";
import { FormErrorText } from "./FormErrorText";

/**
 * Props for the Form component.
 *
 * Extends react-hook-form's FormProviderProps with an optional `onSubmit` prop.
 * Always renders a `<form>` HTML element:
 *
 * - `onSubmit={handler}` — `<form onSubmit={handler}>` (traditional form submission)
 * - no `onSubmit` — `<form onSubmit={e => e.preventDefault()}>` (action-based mode, submission handled by SubmitButton)
 */
interface FormProps<TFieldValues extends FieldValues, TContext, TTransformedValues = TFieldValues> extends FormProviderProps<
  TFieldValues,
  TContext,
  TTransformedValues
> {
  onSubmit?: (e?: React.BaseSyntheticEvent) => void | Promise<void>;
  className?: string;
}

/**
 * Creates a new Form with [FormProvider], wraps children in a `<form>` element,
 * and adds [FormDebug] for debugging purposes.
 */
export function Form<TFieldValues extends FieldValues, TContext, TTransformedValues = TFieldValues>({
  children,
  onSubmit,
  className,
  ...props
}: FormProps<TFieldValues, TContext, TTransformedValues>): React.ReactElement {
  return (
    <FormProvider {...props}>
      <form
        className={className}
        onSubmit={
          onSubmit ??
          ((e) => {
            e.preventDefault();
          })
        }
      >
        {children}
        <FormDebug />
      </form>
    </FormProvider>
  );
}

/**
 * Form Item
 */

interface FormItemContextValue {
  id: string;
}

const FormItemContext = createContext<FormItemContextValue>({} as FormItemContextValue);

export function FormItem({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  const id = useId();
  const contextValue = useMemo(() => ({ id }), [id]);

  return (
    <FormItemContext.Provider value={contextValue}>
      <div className={cn("flex flex-col gap-1", className)} {...props} />
    </FormItemContext.Provider>
  );
}

/**
 * Form Field Context
 */

interface FormFieldContextValue<
  TFieldValues extends FieldValues = FieldValues,
  TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>,
> {
  name: TName;
}

const FormFieldContext = createContext<FormFieldContextValue | undefined>(undefined);

/**
 * Form Field
 */

export const FormField = <TFieldValues extends FieldValues = FieldValues, TName extends FieldPath<TFieldValues> = FieldPath<TFieldValues>>(
  props: ControllerProps<TFieldValues, TName>,
) => {
  const contextValue = useMemo(() => ({ name: props.name }), [props.name]);
  return (
    <FormFieldContext.Provider value={contextValue}>
      <Controller {...props} />
    </FormFieldContext.Provider>
  );
};

/**
 * useFormField Hook
 */

export function useFormField() {
  const fieldContext = useContext(FormFieldContext);
  const itemContext = useContext(FormItemContext);
  const { getFieldState, formState } = useFormContext();

  if (!fieldContext) {
    throw new Error("useFormField should be used within <FormField>");
  }

  const fieldState = getFieldState(fieldContext.name, formState);

  const { id } = itemContext;

  return {
    id,
    name: fieldContext.name,
    formItemId: `${id}-form-item`,
    formLabelId: `${id}-form-item-label`,
    formDescriptionId: `${id}-form-item-description`,
    formMessageId: `${id}-form-item-message`,
    ...fieldState,
  };
}

/**
 * Returns the name of the current form field from the field context.
 */
export function useFormFieldName(): FieldPath<FieldValues> {
  const fieldContext = useContext(FormFieldContext);

  if (!fieldContext) {
    throw new Error("useFormFieldName should be used within <FormField>");
  }

  return fieldContext.name;
}

/**
 * Form Label
 */

export function FormLabel({ className, ...props }: React.ComponentProps<typeof LabelPrimitive.Root>): React.ReactElement {
  const { formItemId } = useFormField();

  return <Label className={className} htmlFor={formItemId} {...props} />;
}

/**
 * Form Control
 *
 * Merges the field's `id` and aria attributes into its child, which must therefore be a single
 * element passing them on to the focusable control — a fragment takes only `key` and `children`.
 */

export function FormControl({ ...props }: React.ComponentProps<typeof Slot.Root>): React.ReactElement {
  const { error, formItemId, formDescriptionId, formMessageId } = useFormField();

  return (
    <Slot.Root
      aria-describedby={!error ? formDescriptionId : `${formDescriptionId} ${formMessageId}`}
      aria-invalid={!!error}
      id={formItemId}
      {...props}
    />
  );
}

/**
 * Form Description
 */

export function FormDescription({ className, ...props }: React.ComponentProps<"p">): React.ReactElement {
  const { formDescriptionId } = useFormField();

  return <p className={cn("text-muted-foreground text-sm", className)} id={formDescriptionId} {...props} />;
}

/**
 * Form Message
 */

export function FormMessage({ className, children, ...props }: React.ComponentProps<"p">): React.ReactElement | null {
  const { error, formMessageId } = useFormField();
  const explicitChildren = isNotDefined(children) ? undefined : children;
  const messageFromError = error?.message;
  const fallbackMessage = isNotDefined(messageFromError) ? undefined : String(messageFromError);
  const body = explicitChildren ?? fallbackMessage;

  if (isNotDefined(body)) {
    return null;
  }

  return (
    <FormErrorText className={className} id={formMessageId} {...props}>
      {body}
    </FormErrorText>
  );
}
