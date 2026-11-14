/**
 * Accessible modal overlay with scrollable body, configurable size variants, and an optional close button.
 */

import { DialogContent as ShadcnDialogContent } from "@neckar-hub/typescript-react-ui-primitives/ui/dialog";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

export {
  Dialog,
  DialogClose,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
  DialogTrigger,
} from "@neckar-hub/typescript-react-ui-primitives/ui/dialog";

/**
 * Forms need more width than the primitive's `sm:max-w-sm`; anchored at `top-16`, the header stays in place while the body
 * grows, and the height limit with the flex column lets `DialogBody` scroll.
 */
const dialogContentVariants = cva("top-16 flex max-h-[calc(100vh-128px)] translate-y-0 flex-col", {
  variants: {
    size: {
      sm: "sm:max-w-lg",
      md: "sm:max-w-3xl",
      lg: "sm:max-w-5xl",
      xl: "sm:max-w-7xl",
      full: "inset-0 max-h-full w-full max-w-full translate-x-0 rounded-none sm:max-w-full",
    },
  },
  defaultVariants: {
    size: "sm",
  },
});

type DialogContentProps = Omit<React.ComponentProps<typeof ShadcnDialogContent>, "onPointerDownOutside"> &
  VariantProps<typeof dialogContentVariants>;

/**
 * Content that stays open on a click outside of it: a dialog holds form input, which a stray click would discard.
 */
export function DialogContent({ className, size, ...props }: DialogContentProps): React.ReactElement {
  return (
    <ShadcnDialogContent
      className={cn(dialogContentVariants({ size }), className)}
      onPointerDownOutside={(event) => event.preventDefault()}
      {...props}
    />
  );
}

/**
 * Scrollable middle section between `DialogHeader` and `DialogFooter`; it takes the height the dialog has left.
 */
export function DialogBody({ className, ...props }: React.ComponentProps<"div">): React.ReactElement {
  return <div data-slot="dialog-body" className={cn("-mx-4 -my-1 min-h-0 flex-1 overflow-y-auto px-4 py-1", className)} {...props} />;
}
