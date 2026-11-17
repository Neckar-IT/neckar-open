/**
 * Imports
 */

import { cn } from "cn";
import { type PropsWithChildren, useId } from "react";
import { type ZodObject } from "zod";
import { RadioGroup, RadioGroupItem } from "../../primitives/radio-group";
import { FormFieldBase, type FormFieldBaseProps } from "../form";
import { FormLabel } from "../form/Form";

/**
 * Type Declarations
 */

type RadioGroupFormFieldProps<T extends ZodObject> = FormFieldBaseProps<T> & PropsWithChildren;

interface RadioGroupFormFieldItemProps extends PropsWithChildren {
  /**
   * The value that is set in the form when the corresponding item is selected
   */
  value: string;
  className?: string;
}

/**
 * Radio button group bound to a react-hook-form field, composable with RadioGroupFormFieldItem children.
 */

export function RadioGroupFormField<T extends ZodObject>({ children, className, ...rest }: RadioGroupFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      className={cn("gap-2", className)}
      render={({ field: { onChange, ...rest } }) => (
        <RadioGroup {...rest} onValueChange={onChange} className="group pl-2">
          {children}
        </RadioGroup>
      )}
    />
  );
}

/**
 * Radio Group Item Component
 */

export function RadioGroupFormFieldItem(props: RadioGroupFormFieldItemProps): React.ReactElement {
  const id = useId();

  return (
    <div className={cn("flex items-center gap-2", props.className)}>
      <RadioGroupItem value={props.value} id={id} />
      <FormLabel htmlFor={id} className="font-normal">
        {props.children}
      </FormLabel>
    </div>
  );
}
