/**
 * Duration Input Component (MM:SS)
 */

/**
 * Imports
 */

import { cn } from "cn";
import { MinusIcon, PlusIcon } from "lucide-react";
import { Button } from "../../primitives/button";
import { Input } from "../../primitives/input";

/**
 * Prop Types
 */

export interface DurationInputProps {
  value: number;
  onChange: (value: number) => void;
  step?: number | undefined;
  min?: number | undefined;
  max?: number | undefined;
  disabled?: boolean | undefined;
  className?: string | undefined;
}

/**
 * MM:SS duration input with increment/decrement buttons, storing the value in milliseconds.
 *
 * TODO: Add a button to clear the field (similar to number input) and allow null | undefined as value
 *
 * @prop {number} value - The value of the input in milliseconds
 * @prop {(value: number) => void} onChange - The function to call when the value changes (receives the new value in milliseconds)
 * @prop {number} [step=1000] - The step to increment or decrement the value by (in milliseconds)
 * @prop {number} [min=0] - The minimum value the input can have (in milliseconds)
 * @prop {number} [max] - The maximum value the input can have (in milliseconds)
 */

export function DurationInput({ value, onChange, step = 1000, min = 0, max, disabled = false, className }: DurationInputProps) {
  const minutes = Math.floor(value / 60000);
  const seconds = Math.round((value % 60000) / 1000);

  const handleIncrement = () => {
    const newValue = max !== undefined && value + step > max ? max : value + step;
    onChange(newValue);
  };

  const handleDecrement = () => {
    const newValue = value - step < min ? min : value - step;
    onChange(newValue);
  };

  const handleChangeMinutes = (value: string) => {
    if (value.length === 0) {
      onChange((0 + seconds) * 1000);
      return;
    }

    onChange((parseInt(value) * 60 + seconds) * 1000);
  };

  const handleChangeSeconds = (value: string) => {
    if (value.length === 0) {
      onChange(minutes * 60 * 1000);
      return;
    }

    if (parseInt(value) >= 60) {
      return;
    }

    onChange((minutes * 60 + parseInt(value)) * 1000);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "ArrowUp") {
      event.preventDefault();
      handleIncrement();
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      handleDecrement();
    }
  };

  return (
    <div
      className={cn(
        "border-input focus-within:ring-ring flex h-10 items-center overflow-hidden rounded-md border focus-within:ring-2 focus-within:outline-hidden disabled:opacity-50",
        className,
      )}
    >
      <Button
        className="border-input focus-visible:bg-accent rounded-r-none border-r focus-visible:ring-0"
        disabled={disabled}
        type="button"
        variant="ghost"
        onClick={handleDecrement}
      >
        <MinusIcon className="h-4 w-4" />
      </Button>
      <div className="flex flex-1 items-center justify-center overflow-hidden">
        <Input
          className="peer h-[38px] flex-auto rounded-none border-0 pr-1 pl-4 text-right focus-visible:ring-0 [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
          disabled={disabled}
          step={Math.floor(step / 60)}
          type="number"
          value={minutes.toString()}
          onChange={(e) => {
            handleChangeMinutes(e.target.value);
          }}
          onKeyDown={handleKeyDown}
        />
        <span className="text-sm peer-disabled:cursor-not-allowed peer-disabled:opacity-50">:</span>
        <Input
          className="peer h-[38px] flex-auto rounded-none border-0 pr-4 pl-1 text-sm focus-visible:ring-0 [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
          disabled={disabled}
          step={step}
          type="number"
          value={seconds < 10 ? `0${seconds.toString()}` : seconds.toString()}
          onChange={(e) => {
            handleChangeSeconds(e.target.value);
          }}
          onKeyDown={handleKeyDown}
        />
      </div>
      <Button
        className="border-input focus-visible:bg-accent rounded-l-none border-l focus-visible:ring-0"
        disabled={disabled}
        type="button"
        variant="ghost"
        onClick={handleIncrement}
      >
        <PlusIcon className="h-4 w-4" />
      </Button>
    </div>
  );
}
