/**
 * Imports
 */

import { cn } from "cn";
import { CalendarIcon } from "lucide-react";
import { useState } from "react";
import { Button } from "../../primitives/button";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { Separator } from "../../primitives/separator";
import { InputWrapper } from "../input-wrapper";

/**
 * Type Declarations
 */

export interface PickerShellQuickSelect {
  title: string;
  onSelect: () => void;
}

export interface PickerShellProps {
  hasValue: boolean;
  /** The content of the default trigger button next to the calendar icon. */
  triggerLabel: React.ReactNode;
  triggerTestId?: string | undefined;
  /** Override the default trigger button. Wrapped in `PopoverTrigger asChild`. */
  trigger?: React.ReactNode | undefined;
  quickSelects?: PickerShellQuickSelect[] | undefined;
  onClear: () => void;
  clearSelectionLabel: string;
  /** The calendar, shown left of the quick selects. A function receives `close` to shut the popover after a pick. */
  children: React.ReactNode | ((close: () => void) => React.ReactNode);
  className?: string | undefined;
  disabled?: boolean | undefined;
  readOnly?: boolean | undefined;
  isInvalid?: boolean | undefined;
  /** The accessible name of the default trigger where no visible label names it. A set value is appended: `Start: 06/28/2024`. */
  ariaLabel?: string | undefined;
  initiallyOpen?: boolean | undefined;
}

/**
 * Popover with trigger, quick-select column and clear footer shared by `DatePicker`,
 * `DateTimePicker` and `DateRangePicker`. The shell knows no value type; the picker
 * supplies the calendar and the trigger text. Quick select and clear close the popover.
 */
export function PickerShell({
  hasValue,
  triggerLabel,
  triggerTestId,
  trigger,
  quickSelects,
  onClear,
  clearSelectionLabel,
  children,
  className,
  disabled = false,
  readOnly = false,
  isInvalid,
  ariaLabel,
  initiallyOpen = false,
}: PickerShellProps): React.ReactElement {
  const [isOpen, setIsOpen] = useState(initiallyOpen);
  const triggerAriaLabel =
    ariaLabel !== undefined && hasValue && typeof triggerLabel === "string" ? `${ariaLabel}: ${triggerLabel}` : ariaLabel;

  return (
    <Popover modal={true} open={isOpen} onOpenChange={setIsOpen}>
      {trigger !== undefined ? (
        <PopoverTrigger asChild>{trigger}</PopoverTrigger>
      ) : (
        <InputWrapper
          className={className}
          disabled={disabled || readOnly}
          hideClearButton={!hasValue}
          isInvalid={isInvalid}
          onClear={onClear}
        >
          <PopoverTrigger asChild>
            <Button
              aria-label={triggerAriaLabel}
              className="focus-visible:bg-accent focus-visible:text-accent-foreground h-[38px] w-full flex-1 justify-start focus-visible:ring-0"
              disabled={disabled || readOnly}
              size="sm"
              variant="ghost"
            >
              <CalendarIcon />
              <span className={cn("font-normal", { "text-muted-foreground": !hasValue })} data-testid={triggerTestId}>
                {triggerLabel}
              </span>
            </Button>
          </PopoverTrigger>
        </InputWrapper>
      )}
      <PopoverContent align="start" className="w-min p-0">
        <div className="flex items-stretch">
          {typeof children === "function" ? children(() => setIsOpen(false)) : children}
          {quickSelects !== undefined && quickSelects.length > 0 ? (
            <div className="space-y-2 border-l p-3">
              {quickSelects.map((quickSelect, index) => (
                <Button
                  key={`quickselect-${index.toString()}`}
                  className="w-full rounded-full px-4 text-center text-sm"
                  size="sm"
                  variant="secondary"
                  onClick={() => {
                    quickSelect.onSelect();
                    setIsOpen(false);
                  }}
                >
                  {quickSelect.title}
                </Button>
              ))}
            </div>
          ) : null}
        </div>
        {hasValue ? (
          <>
            <Separator orientation="horizontal" />
            <div className="p-1">
              <Button
                className="w-full"
                size="sm"
                variant="ghost"
                onClick={() => {
                  onClear();
                  setIsOpen(false);
                }}
              >
                {clearSelectionLabel}
              </Button>
            </div>
          </>
        ) : null}
      </PopoverContent>
    </Popover>
  );
}
