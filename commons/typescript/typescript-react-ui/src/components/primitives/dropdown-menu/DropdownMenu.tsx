/**
 * Trigger-anchored menu with items, checkboxes, radio groups, sub-menus, and a destructive item variant.
 */

import {
  DropdownMenuCheckboxItem as ShadcnDropdownMenuCheckboxItem,
  DropdownMenuItem as ShadcnDropdownMenuItem,
  DropdownMenuRadioItem as ShadcnDropdownMenuRadioItem,
  DropdownMenuSubTrigger as ShadcnDropdownMenuSubTrigger,
} from "@neckar-hub/typescript-react-ui-primitives/ui/dropdown-menu";
import { cn } from "cn";

export {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuPortal,
  DropdownMenuRadioGroup,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuTrigger,
} from "@neckar-hub/typescript-react-ui-primitives/ui/dropdown-menu";

/*
 * Item, checkbox item, radio item and sub-menu trigger carry a pointer cursor on hover: they render a `div` with `role="menuitem…"`, which the
 * button cursor rule of `@neckar-hub/tailwind-config` does not reach.
 */

export function DropdownMenuItem({ className, ...props }: React.ComponentProps<typeof ShadcnDropdownMenuItem>): React.ReactElement {
  return <ShadcnDropdownMenuItem className={cn("hover:cursor-pointer", className)} {...props} />;
}

export function DropdownMenuCheckboxItem({
  className,
  ...props
}: React.ComponentProps<typeof ShadcnDropdownMenuCheckboxItem>): React.ReactElement {
  return <ShadcnDropdownMenuCheckboxItem className={cn("hover:cursor-pointer", className)} {...props} />;
}

export function DropdownMenuRadioItem({
  className,
  ...props
}: React.ComponentProps<typeof ShadcnDropdownMenuRadioItem>): React.ReactElement {
  return <ShadcnDropdownMenuRadioItem className={cn("hover:cursor-pointer", className)} {...props} />;
}

export function DropdownMenuSubTrigger({
  className,
  ...props
}: React.ComponentProps<typeof ShadcnDropdownMenuSubTrigger>): React.ReactElement {
  return <ShadcnDropdownMenuSubTrigger className={cn("hover:cursor-pointer", className)} {...props} />;
}
