/**
 * Imports
 */

import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import type { PropsWithChildren } from "react";
import { ClearButton } from "../../actions/clear-button";
import { Separator } from "../../primitives/separator";

/**
 * Prop Types
 */

export interface InputWrapperProps extends PropsWithChildren {
  className?: string;
  hideClearButton?: boolean;
  onClear: () => void;
  isInvalid?: boolean;
  disabled?: boolean;
  customClearIcon?: React.ReactElement;
  addOnLeft?: React.ReactNode;
  addOnRight?: React.ReactNode;
}

/**
 * Component that wraps any input and renders a border as well as a clear button
 */

export function InputWrapper({
  className,
  children,
  hideClearButton,
  disabled,
  isInvalid,
  onClear,
  addOnLeft,
  addOnRight,
}: InputWrapperProps): React.ReactElement {
  return (
    <div
      className={cn(
        "border-input bg-background flex h-10 w-full items-center overflow-hidden rounded-md border",
        "focus-within:ring-ring focus-within:ring-2",
        { "border-border/50 opacity-50": disabled, "border-destructive focus-within:ring-destructive/30": isInvalid },
        className,
      )}
    >
      <AddOnLeft addOnLeft={addOnLeft} disabled={disabled} />
      <div className="flex-1 overflow-hidden">{children}</div>
      <AddOnRight addOnRight={addOnRight} hideClearButton={hideClearButton} disabled={disabled} onClear={onClear} />
    </div>
  );
}

/**
 * Separate Component for the Right Addon
 * Will be a button to clear the input by default
 */

export function AddOnRight({
  addOnRight,
  hideClearButton,
  disabled,
  onClear,
}: {
  hideClearButton?: boolean;
  addOnRight?: React.ReactNode;
  disabled?: boolean;
  onClear: () => void;
}): React.ReactElement | null {
  if (isDefined(addOnRight)) {
    return (
      <>
        <Separator orientation="vertical" />
        {addOnRight}
      </>
    );
  }

  if (hideClearButton) {
    return null;
  }

  return (
    <>
      <Separator orientation="vertical" className={cn({ "opacity-50": disabled })} />
      <ClearButton disabled={disabled} onClick={onClear} className="shrink-0 rounded-s-none" />
    </>
  );
}

export function AddOnLeft({ addOnLeft, disabled }: { addOnLeft?: React.ReactNode; disabled?: boolean }): React.ReactElement | null {
  if (isDefined(addOnLeft)) {
    return (
      <>
        {addOnLeft}
        <Separator orientation="vertical" className={cn({ "opacity-50": disabled })} />
      </>
    );
  }

  return null;
}
