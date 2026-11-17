/**
 * Floating popover anchored to a trigger element, with optional lock-open behavior.
 */

import { PopoverContent as ShadcnPopoverContent } from "@neckar-hub/typescript-react-ui-primitives/ui/popover";

export { Popover, PopoverTrigger } from "@neckar-hub/typescript-react-ui-primitives/ui/popover";

type PopoverContentProps = React.ComponentProps<typeof ShadcnPopoverContent> & {
  /** Keeps the popover open on a pointer down outside of it. */
  preventCloseOnClickOutside?: boolean;
  /** Keeps the popover open when the focus moves outside of it. */
  preventCloseOnFocusOutside?: boolean;
};

/**
 * Named props for the outside-dismiss cases read better than `event.preventDefault()` at every call site; a caller's
 * handler still runs.
 */
export function PopoverContent({
  preventCloseOnClickOutside = false,
  preventCloseOnFocusOutside = false,
  onPointerDownOutside,
  onFocusOutside,
  ...props
}: PopoverContentProps): React.ReactElement {
  return (
    <ShadcnPopoverContent
      onPointerDownOutside={(event) => {
        if (preventCloseOnClickOutside) {
          event.preventDefault();
        }
        onPointerDownOutside?.(event);
      }}
      onFocusOutside={(event) => {
        if (preventCloseOnFocusOutside) {
          event.preventDefault();
        }
        onFocusOutside?.(event);
      }}
      {...props}
    />
  );
}
