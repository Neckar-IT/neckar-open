import { Button, DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@neckar-hub/typescript-react-ui/actions";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { EllipsisVerticalIcon } from "lucide-react";
import type { Action } from "./Action";
import { useActionState } from "./useActionState";

interface ActionsDropdownProps extends React.PropsWithChildren {
  className?: string;
  /** The accessible name of the trigger button. */
  triggerLabel?: string;
  /** The variant of the trigger button. */
  triggerVariant?: "ghost" | "outline";
}

/**
 * Dropdown menu grouping several `Action`s behind a single trigger button.
 */
export function ActionsDropdown({ className, triggerLabel, triggerVariant = "ghost", children }: ActionsDropdownProps): React.ReactElement {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={triggerVariant} size="icon" className={className} aria-label={triggerLabel}>
          <EllipsisVerticalIcon />
        </Button>
      </DropdownMenuTrigger>
      {/* The menu takes the width of its longest item instead of the icon trigger's; only a label longer than `max-w-xs` wraps. */}
      <DropdownMenuContent className="w-max max-w-xs">{children}</DropdownMenuContent>
    </DropdownMenu>
  );
}

interface ActionsDropdownItemProps {
  action: Action;
}

/**
 * Single `Action` rendered as an item inside an `ActionsDropdown`. A disabled item names its reason below the label, because a
 * disabled menu item receives no hover for a tooltip.
 */
export function ActionsDropdownItem({ action }: ActionsDropdownItemProps): React.ReactElement | null {
  const state = useActionState(action);

  if (action.hidden) {
    return null;
  }

  // DropdownMenuItem only supports "default" | "destructive", map other variants to default
  const dropdownVariant = action.variant === "destructive" ? "destructive" : undefined;

  return (
    <DropdownMenuItem onSelect={action.onTrigger} variant={dropdownVariant} disabled={state.blocked}>
      {action.icon}
      {state.disabled && isDefined(state.disabledReason) ? (
        <span className="flex flex-col">
          <span>{action.label}</span>
          <span className="text-muted-foreground text-xs">{state.disabledReason}</span>
        </span>
      ) : (
        action.label
      )}
    </DropdownMenuItem>
  );
}
