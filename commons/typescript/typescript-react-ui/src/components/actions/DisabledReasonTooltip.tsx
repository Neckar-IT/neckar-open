import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@neckar-hub/typescript-react-ui/overlays";
import { isDefined } from "@neckar-hub/typescript-utils/lang";

interface DisabledReasonTooltipProps {
  /** Why the wrapped button is disabled; `undefined` renders the button alone. */
  reason: React.ReactNode;
  children: React.ReactElement;
}

export function DisabledReasonTooltip({ reason, children }: DisabledReasonTooltipProps): React.ReactElement {
  if (!isDefined(reason)) {
    return children;
  }
  // A disabled button fires no pointer events, so the tooltip hangs on a wrapper the button fills; the own provider keeps
  // the button usable in an app that mounts none.
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild tabIndex={0}>
          <span className="inline-flex *:flex-1">{children}</span>
        </TooltipTrigger>
        <TooltipContent>{reason}</TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
