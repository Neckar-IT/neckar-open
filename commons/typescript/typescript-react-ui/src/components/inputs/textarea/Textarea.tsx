/**
 * Imports
 */

import { cn } from "cn";
import { type ChangeEventHandler } from "react";

/**
 * Prop Types
 */

interface TextareaProps extends Omit<React.ComponentProps<"textarea">, "value" | "defaultValue" | "onChange"> {
  onChange: (value: string | null) => void;
  value: string | null | undefined;
  isInvalid?: boolean;
  resizable?: boolean;
}

/**
 * Multi-line text input with null-as-empty normalization and optional resize handle.
 */

export function Textarea({ value, onChange, isInvalid, className, resizable = false, ...rest }: TextareaProps): React.ReactElement {
  const sanitizedValue = value ?? "";
  const handleChange: ChangeEventHandler<HTMLTextAreaElement> = (event) => {
    const newValue = event.target.value;
    if (newValue === "") {
      onChange(null);
    } else {
      onChange(newValue);
    }
  };

  return (
    <textarea
      value={sanitizedValue}
      onChange={handleChange}
      className={cn(
        "border-input bg-background ring-offset-background placeholder:text-muted-foreground focus-visible:ring-ring flex min-h-20 w-full rounded-md border px-3 py-2 text-sm focus-visible:ring-2 focus-visible:outline-hidden disabled:cursor-not-allowed disabled:opacity-50",
        { "border-destructive focus-visible:ring-destructive/30": isInvalid, "resize-none": !resizable },
        className,
      )}
      {...rest}
    />
  );
}
