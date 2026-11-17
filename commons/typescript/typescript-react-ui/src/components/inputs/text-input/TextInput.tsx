/**
 * Imports
 */

import type { ChangeEventHandler } from "react";
import { InputWrapper, type InputWrapperProps } from "../input-wrapper";

/**
 * Prop Types
 */

export type TextInputProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "defaultValue" | "onChange" | "type"> &
  Omit<InputWrapperProps, "onClear"> & {
    type?: "text" | "email" | "url";
    onChange: (value: string | null) => void;
    value: string | null;
  };

/**
 * Single-line text input (text, email, or url) with a clear button and optional left/right add-ons.
 */

export function TextInput({
  value,
  onChange,
  isInvalid,
  type = "text",
  disabled,
  className,
  customClearIcon,
  addOnLeft,
  addOnRight,
  ...rest
}: TextInputProps): React.ReactElement {
  const handleChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    const newValue = event.target.value;
    onChange(newValue === "" ? null : newValue);
  };

  const handleClear = () => {
    onChange(null);
  };

  return (
    <InputWrapper
      disabled={disabled}
      className={className}
      onClear={handleClear}
      hideClearButton={!value}
      isInvalid={isInvalid}
      customClearIcon={customClearIcon}
      addOnLeft={addOnLeft}
      addOnRight={addOnRight}
    >
      <input
        value={value ?? ""}
        onChange={handleChange}
        type={type}
        disabled={disabled}
        className="placeholder:text-muted-foreground w-full px-3 py-2 text-sm focus-visible:outline-hidden disabled:cursor-not-allowed"
        {...rest}
      />
    </InputWrapper>
  );
}
