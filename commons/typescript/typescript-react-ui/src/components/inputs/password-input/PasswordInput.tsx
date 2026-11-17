/**
 * Imports
 */

import { EyeIcon, EyeOffIcon } from "lucide-react";
import { useState, type ChangeEventHandler } from "react";
import { Button } from "../../primitives/button";
import { InputWrapper } from "../input-wrapper";

/**
 * Prop Types
 */

type PasswordInputProps = Omit<React.ComponentProps<"input">, "value" | "defaultValue" | "onChange" | "type"> & {
  isInvalid?: boolean;
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  autoComplete?: "current-password" | "new-password";
};

/**
 * Password input with show/hide toggle and clear button.
 */

export function PasswordInput({ value, onChange, isInvalid, className, ...rest }: PasswordInputProps): React.ReactElement {
  const [showPassword, setShowPassword] = useState(false);

  const handleChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    const newValue = event.target.value;
    onChange(newValue === "" ? null : newValue);
  };

  const handleClear = () => {
    onChange(null);
  };

  return (
    <InputWrapper disabled={rest.disabled} className={className} onClear={handleClear} hideClearButton={!value} isInvalid={isInvalid}>
      <div className="flex flex-1 items-center">
        <input
          {...rest}
          className="placeholder:text-muted-foreground flex-1 px-3 py-2 text-sm focus-visible:outline-hidden disabled:cursor-not-allowed"
          type={showPassword ? "text" : "password"}
          value={value ?? ""}
          onChange={handleChange}
        />
        <Button variant="ghost" size="icon" className="h-[38px] w-[38px]" tabIndex={-1} onClick={() => setShowPassword((prev) => !prev)}>
          {showPassword ? <EyeOffIcon /> : <EyeIcon />}
        </Button>
      </div>
    </InputWrapper>
  );
}
