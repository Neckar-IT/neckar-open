/**
 * Side-drawer dialog that slides in from any edge of the viewport over existing content.
 */

import { cn } from "cn";
import { Dialog as SheetPrimitive } from "radix-ui";

export {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@neckar-hub/typescript-react-ui-primitives/ui/sheet";

/*
 * The primitive does not export `SheetPortal` and `SheetOverlay`; both are rebuilt here on the same Radix parts to keep the
 * exports. The overlay color comes from the `[data-slot=sheet-overlay]` rule in `@neckar-hub/tailwind-config`.
 */

export function SheetPortal(props: React.ComponentProps<typeof SheetPrimitive.Portal>): React.ReactElement {
  return <SheetPrimitive.Portal data-slot="sheet-portal" {...props} />;
}

export function SheetOverlay({ className, ...props }: React.ComponentProps<typeof SheetPrimitive.Overlay>): React.ReactElement {
  return (
    <SheetPrimitive.Overlay
      data-slot="sheet-overlay"
      className={cn(
        "data-open:animate-in data-open:fade-in-0 data-closed:animate-out data-closed:fade-out-0 fixed inset-0 z-50 duration-100 supports-backdrop-filter:backdrop-blur-xs",
        className,
      )}
      {...props}
    />
  );
}
