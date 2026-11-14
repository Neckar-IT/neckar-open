/**
 * Imports
 */

import { cn } from "cn";
import { type PropsWithChildren } from "react";
import { type ZodObject } from "zod";
import { ToggleGroup, ToggleGroupItem } from "../../primitives/toggle-group";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Type Declarations
 */

type ToggleGroupFormFieldProps<T extends ZodObject> = FormFieldBaseProps<T> & PropsWithChildren;

interface ToggleGroupFormFieldItemProps extends PropsWithChildren {
  /**
   * The value that is set in the form when the corresponding item is selected
   */
  value: string;
  className?: string;
}

/**
 * React-hook-form field wrapper around ToggleGroup for single-selection from a small fixed set of options.
 */

export function ToggleGroupFormField<T extends ZodObject>({ children, className, ...rest }: ToggleGroupFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      className={cn("gap-2", className)}
      render={({ field: { onChange, value, ...rest } }) => (
        <ToggleGroup
          type="single"
          variant="outline"
          className="justify-start"
          value={value as string}
          onValueChange={(newValue) => {
            // Prevent deselection — always keep one item selected
            if (newValue) {
              onChange(newValue);
            }
          }}
          {...rest}
        >
          {children}
        </ToggleGroup>
      )}
    />
  );
}

/**
 * Toggle Group Item Component
 */

export function ToggleGroupFormFieldItem(props: ToggleGroupFormFieldItemProps): React.ReactElement {
  return (
    <ToggleGroupItem value={props.value} className={cn("px-4", props.className)}>
      {props.children}
    </ToggleGroupItem>
  );
}
