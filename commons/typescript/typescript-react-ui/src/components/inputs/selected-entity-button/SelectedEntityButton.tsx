import { cn } from "cn";
import type { ComponentProps, ReactElement } from "react";
import { activeFilterSurface } from "../../filters/filter-trigger";
import { Button } from "../../primitives/button";
import { InputWrapper } from "../input-wrapper";

export interface SelectedEntityButtonProps extends ComponentProps<"button"> {
  onClear: () => void;
  /** Hides the X while no entity is selected. */
  hideClearButton: boolean;
  /** Shows the selection in the surface of an active filter button: blue text, light blue fill, blue border. */
  marked?: boolean;
}

/**
 * The button that shows the selected entity and opens its select dialog, inside an `InputWrapper` with the clear X.
 * Spreads the remaining props onto the button, so a `FormControl` can slot onto it.
 */
export function SelectedEntityButton({
  onClear,
  hideClearButton,
  marked = false,
  children,
  className,
  ...buttonProps
}: SelectedEntityButtonProps): ReactElement {
  return (
    <InputWrapper onClear={onClear} hideClearButton={hideClearButton} className={marked ? activeFilterSurface : undefined}>
      <Button
        variant="ghost"
        className={cn(
          "h-[38px] w-full justify-start font-normal",
          marked ? "hover:text-primary bg-transparent hover:bg-transparent" : null,
          className,
        )}
        {...buttonProps}
      >
        {children}
      </Button>
    </InputWrapper>
  );
}
