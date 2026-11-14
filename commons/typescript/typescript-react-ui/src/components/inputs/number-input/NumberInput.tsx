/**
 * Number Input Component
 *
 * Copied over from OriginUI (https://originui.com/inputs) and modified to fit the other component's style.
 */

/**
 * Imports
 */

import { useNumberFormatter, useNumberParser } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { MinusIcon, PlusIcon } from "lucide-react";
import { type FocusEventHandler, useCallback, useEffect, useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";
import { AddOnLeft, AddOnRight } from "../input-wrapper/InputWrapper";
import { normalizeFloatDrift } from "./normalizeFloatDrift";
import { useQuantityFormatter } from "./useQuantityFormatter";

/**
 * Number Input Component
 */

export interface NumberInputProps {
  /**
   * The current value of the input as seen by its parent component
   */
  value: number | undefined;
  onChange: (value: number | undefined) => void;
  step?: number | undefined;
  min?: number | undefined;
  max?: number | undefined;
  disabled?: boolean | undefined;
  className?: string | undefined;
  fractionDigits?: number | undefined;
  unit?: string;
  unitPosition?: "prefix" | "suffix";
  /**
   * Factor to convert the actual value to the displayed value
   */
  conversionFactor?: number;
  showButtons?: boolean;
  isInvalid?: boolean;
  addOnLeft?: React.ReactNode;
  addOnRight?: React.ReactNode;
  hideClearButton?: boolean;
  /**
   * Whether to use grouping separators (e.g. "1.000" vs "1000").
   * Defaults to `true`. Set to `false` for values like years where grouping is misleading.
   */
  useGrouping?: boolean;
  /** The accessible name of the field where no visible label names it. */
  ariaLabel?: string | undefined;
  /** Shown while the field is empty. */
  placeholder?: string | undefined;
}

/**
 * Locale-aware numeric input with optional increment/decrement buttons, unit display, and clear action.
 */
export function NumberInput({
  value,
  onChange,
  step = 1,
  min,
  max,
  disabled,
  className,
  fractionDigits,
  unit,
  unitPosition = "suffix",
  conversionFactor = 1,
  showButtons = false,
  isInvalid,
  addOnLeft,
  addOnRight,
  hideClearButton,
  useGrouping,
  ariaLabel,
  placeholder,
}: NumberInputProps) {
  const format = useNumberFormatter(fractionDigits, useGrouping);
  const formatQuantity = useQuantityFormatter({ fractionDigits, conversionFactor, useGrouping });
  const parse = useNumberParser();
  const { t } = useEmbeddedTranslation();

  const formatIfDefined = useCallback(
    (value: number | undefined) => {
      if (value !== undefined) {
        return formatQuantity(value);
      } else {
        return "";
      }
    },
    [formatQuantity],
  );

  // The rawValue is the string that is actually shown in the input field
  const [rawValue, setRawValue] = useState<string>(() => formatIfDefined(value));

  // synchronize the input value with the value prop

  useEffect(() => {
    setRawValue(formatIfDefined(value));
  }, [formatIfDefined, value, setRawValue]);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRawValue(event.target.value);
  };

  const hasValue = value !== undefined;

  const handleDecrement = () => {
    if (value === undefined) {
      onChange(min ?? 0);
      return;
    }

    const newValue = value - step;
    if (min !== undefined && newValue < min) {
      onChange(min);
    } else {
      onChange(newValue);
    }
  };

  const handleIncrement = () => {
    if (value === undefined) {
      onChange(max ?? 0);
      return;
    }

    const newValue = value + step;
    if (max !== undefined && newValue > max) {
      onChange(max);
    } else {
      onChange(newValue);
    }
  };

  const handleBlur = () => {
    const parsedValue = parse(rawValue, fractionDigits);
    if (parsedValue === null) {
      onChange(undefined);
      setRawValue("");
      return;
    }

    onChange(normalizeFloatDrift(parsedValue / conversionFactor));
    setRawValue(format(parsedValue));
  };

  const handleKeyUp = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "ArrowUp") handleIncrement();
    if (event.key === "ArrowDown") handleDecrement();
    if (event.key === "Enter") handleBlur();
  };

  const handleFocus: FocusEventHandler<HTMLInputElement> = (event) => {
    event.target.select();
  };

  const handleClear = () => {
    onChange(undefined);
    setRawValue("");
  };

  return (
    <div
      className={cn(
        "border-input bg-background flex h-10 w-full items-center overflow-hidden rounded-md border",
        "focus-within:ring-ring focus-within:ring-2",
        {
          "border-border/50": disabled,
          "border-destructive focus-within:ring-destructive/30": isInvalid,
        },
        className,
      )}
      onKeyUp={handleKeyUp}
    >
      <AddOnLeft addOnLeft={addOnLeft} disabled={disabled} />
      {showButtons ? (
        <Button
          className="focus-visible:bg-accent focus-visible:text-accent-foreground shrink-0 rounded-none border-r focus-visible:ring-0"
          disabled={disabled}
          type="button"
          variant="ghost"
          onClick={handleDecrement}
          tabIndex={-1}
          aria-label={t("numberInput.decrease")}
          size="icon"
        >
          <MinusIcon aria-hidden />
        </Button>
      ) : null}
      <div className={cn("flex flex-1 items-center", { "opacity-50": disabled })}>
        {unit && unitPosition === "prefix" ? <div className="text-muted-foreground shrink-0 pl-3 text-sm">{unit}</div> : null}
        <input
          className={cn(
            "placeholder:text-muted-foreground w-16 flex-1 px-3 py-2 text-sm focus-visible:outline-hidden disabled:cursor-not-allowed",
            {
              "text-center": showButtons && !unit,
              "text-left": !!unit && unitPosition === "prefix",
              "text-right": !!unit && unitPosition === "suffix",
            },
          )}
          aria-label={ariaLabel}
          placeholder={placeholder}
          disabled={disabled}
          inputMode={fractionDigits === 0 ? "numeric" : "decimal"}
          type="text"
          value={rawValue}
          onBlur={handleBlur}
          onChange={handleChange}
          onFocus={handleFocus}
        />
        {unit && unitPosition === "suffix" ? <div className="text-muted-foreground shrink-0 pr-3 text-sm">{unit}</div> : null}
      </div>
      {showButtons ? (
        <Button
          className="focus-visible:bg-accent focus-visible:text-accent-foreground shrink-0 rounded-none border-l focus-visible:ring-0"
          disabled={disabled}
          type="button"
          variant="ghost"
          onClick={handleIncrement}
          tabIndex={-1}
          aria-label={t("numberInput.increase")}
          size="icon"
        >
          <PlusIcon aria-hidden />
        </Button>
      ) : null}
      <AddOnRight addOnRight={addOnRight} onClear={handleClear} hideClearButton={!hasValue || hideClearButton} disabled={disabled} />
    </div>
  );
}
