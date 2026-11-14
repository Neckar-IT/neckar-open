/**
 * Imports
 */

import { cn } from "cn";
import { Slot } from "radix-ui";

/**
 * Prop Types
 */

export interface UnstyledButtonProps extends React.ComponentProps<"button"> {
  /**
   * When true, renders as a Slot that forwards props to its single child
   * instead of emitting a <button> element.
   */
  asChild?: boolean;
}

/**
 * Behavior-only button with focus ring and disabled state but no visual surface (no background, border, padding, or font styling).
 *
 * Use when you need a button-shaped hit area around arbitrary content (icon,
 * badge, link-styled text, custom composition) and the Button primitive's
 * styling would overlay the content.
 *
 * For standard buttons with project styling, use the Button primitive instead.
 */

function UnstyledButton({ className, asChild = false, type = "button", ...rest }: UnstyledButtonProps) {
  const Comp = asChild ? Slot.Root : "button";

  return (
    <Comp
      type={type}
      className={cn(
        "ring-offset-background focus-visible:ring-ring cursor-pointer focus-visible:ring-2 focus-visible:ring-offset-0 focus-visible:outline-hidden disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
      {...rest}
    />
  );
}
UnstyledButton.displayName = "UnstyledButton";

/**
 * Export
 */

export { UnstyledButton };
