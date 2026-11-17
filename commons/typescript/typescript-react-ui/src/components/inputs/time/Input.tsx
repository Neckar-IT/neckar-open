/**
 * Imports
 */

import type { ChangeEventHandler } from "react";
import { InputWrapper } from "../input-wrapper";

/**
 * Prop Types
 */

export interface TimeInputProps extends Omit<React.ComponentProps<"input">, "value" | "defaultValue" | "onChange"> {
  value: string | undefined | null;
  onChange: (value: string | null) => void;
  includeSeconds?: boolean;
  isInvalid?: boolean;
  /**
   * Force the X-clear button off, even when a value is set. Use when the input lives
   * inside a larger picker (e.g. `DateTimePicker`) that owns clearing at a higher level.
   * Default: clear is shown whenever a value is set.
   */
  hideClearButton?: boolean;
}

/**
 * Time-of-day input for an HH:MM (or HH:MM:SS) string value, with an integrated clear button.
 */

export function TimeInput({
  value,
  onChange,
  isInvalid,
  className,
  includeSeconds = false,
  disabled,
  readOnly,
  hideClearButton,
  ...props
}: TimeInputProps) {
  const thisValue = includeSeconds ? value : value?.slice(0, 5);

  const handleChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    let value = event.target.value;

    if (!includeSeconds) {
      value = `${value}:00`;
    }
    onChange(value);
  };

  const handleClear = () => {
    onChange(null);
  };

  return (
    <InputWrapper
      hideClearButton={hideClearButton ?? !value}
      onClear={handleClear}
      isInvalid={isInvalid}
      disabled={disabled || readOnly}
      className={className}
    >
      <input
        className="placeholder:text-muted-foreground w-full px-3 py-2 text-sm focus-visible:outline-hidden disabled:cursor-not-allowed"
        disabled={disabled || readOnly}
        readOnly={readOnly}
        step={includeSeconds ? 1 : 60}
        type="time"
        value={thisValue ?? ""}
        onChange={handleChange}
        {...props}
      />
    </InputWrapper>
  );
}
