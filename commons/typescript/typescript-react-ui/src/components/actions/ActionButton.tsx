import { Button, type ButtonProps } from "@neckar-hub/typescript-react-ui/actions";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@neckar-hub/typescript-react-ui/overlays";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { Action } from "./Action";
import { useActionState } from "./useActionState";

export interface ActionButtonProps extends Omit<ButtonProps, "onClick" | "children"> {
  action: Action;
}

/**
 * Button that renders a single `Action` (label, icon, click handler, loading state).
 *
 * A lock from `ActionLockProvider` disables it; the lock reason, else `action.disabledReason`, is its tooltip while disabled.
 * A click while loading does nothing, so a running action is never triggered twice.
 */
export function ActionButton({ action, variant, disabled, loading, ...buttonProps }: ActionButtonProps): React.ReactElement | null {
  const state = useActionState(action, { disabled, loading });

  if (action.hidden) {
    return null;
  }

  const button = (
    <Button
      onClick={action.onTrigger}
      variant={variant ?? action.variant}
      disabled={state.blocked}
      loading={state.loading}
      {...buttonProps}
    >
      {action.label}
    </Button>
  );

  if (state.disabled && isDefined(state.disabledReason)) {
    // A disabled button fires no pointer events, so the tooltip hangs on a wrapper; the own provider keeps the button usable
    // in an app that mounts none.
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild tabIndex={0}>
            <span className="inline-flex">{button}</span>
          </TooltipTrigger>
          <TooltipContent>{state.disabledReason}</TooltipContent>
        </Tooltip>
      </TooltipProvider>
    );
  }

  if (isDefined(action.hoverContent)) {
    return (
      <HoverCard openDelay={10} closeDelay={100}>
        <HoverCardTrigger asChild>{button}</HoverCardTrigger>
        <HoverCardContent className="w-sm text-sm">{action.hoverContent}</HoverCardContent>
      </HoverCard>
    );
  }

  return button;
}
