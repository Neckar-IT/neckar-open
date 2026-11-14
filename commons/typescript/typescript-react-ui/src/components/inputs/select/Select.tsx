/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import type { PropsWithChildren, ReactElement } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { buttonVariants } from "../../primitives/button";
import { SelectContent, Select as SelectRoot, SelectTrigger, SelectValue } from "../../primitives/select";
import { InputWrapper } from "../input-wrapper";

/**
 * Prop Types
 */

export type SelectProps = PropsWithChildren<{
  className?: string;
  value: string | null;
  onChange: (value: string | null) => void;
  disabled?: boolean;
  isInvalid?: boolean;
  hideClearButton?: boolean;
  /**
   * placeholder will be shown, when no selection was made (it is a generic string that urges the user to make a selection)
   */
  placeholder?: TextKey;
  /**
   * Rendered left of the trigger, separated by a divider — mirrors [TextInput]. A defined add-on
   * takes the slot's space; use it for an info popover or a leading icon.
   */
  addOnLeft?: React.ReactNode;
  /**
   * Rendered right of the trigger, separated by a divider — mirrors [TextInput]. A defined add-on
   * replaces the built-in clear button; use it for a mode toggle.
   */
  addOnRight?: React.ReactNode;
}>;

/**
 * Single-value select with an integrated clear button, wrapping the primitives Select with InputWrapper styling.
 */

export function Select({
  className,
  value,
  onChange,
  disabled,
  isInvalid,
  children,
  hideClearButton,
  placeholder,
  addOnLeft,
  addOnRight,
}: SelectProps): ReactElement {
  const { t } = useEmbeddedTranslation();

  const handleChange = (newValue: string) => {
    onChange(newValue === "" ? null : newValue);
  };

  const handleClear = () => {
    onChange(null);
  };

  return (
    <SelectRoot value={value ?? ""} onValueChange={handleChange} disabled={disabled}>
      <InputWrapper
        className={className}
        onClear={handleClear}
        hideClearButton={hideClearButton || !value}
        disabled={disabled}
        isInvalid={isInvalid}
        addOnLeft={addOnLeft}
        addOnRight={addOnRight}
      >
        <SelectTrigger
          className={cn(
            buttonVariants({ variant: "ghost" }),
            "w-full justify-between border-0 font-normal focus:ring-0 focus-visible:ring-0",
          )}
        >
          <SelectValue placeholder={t(placeholder ?? "select.placeholder")} />
        </SelectTrigger>
      </InputWrapper>
      <SelectContent>{children}</SelectContent>
    </SelectRoot>
  );
}
