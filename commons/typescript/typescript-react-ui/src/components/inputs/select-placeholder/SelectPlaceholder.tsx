/**
 * Imports
 */

import { cn } from "cn";
import { AlertCircleIcon, XCircleIcon } from "lucide-react";
import { Select, SelectTrigger, SelectValue } from "../../primitives/select";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";

/**
 * Prop Types
 */

export type SelectPlaceholderProps =
  | {
      className?: string | undefined;
      placeholder: string;
      variant?: "default";
    }
  | {
      className?: string | undefined;
      placeholder: string;
      variant: "error" | "warning";
      message: string;
    };

/**
 * Disabled select trigger shown while options are loading, with optional error or warning tooltip.
 */

export function SelectPlaceholder(props: SelectPlaceholderProps): React.ReactElement {
  return (
    <div className={cn("flex items-center gap-2", props.className)}>
      <Select disabled={true}>
        <SelectTrigger className="flex-1">
          <SelectValue placeholder={props.placeholder} />
        </SelectTrigger>
      </Select>

      {props.variant === "error" || props.variant === "warning" ? (
        <Tooltip delayDuration={250}>
          <TooltipTrigger>
            {props.variant === "error" ? <XCircleIcon className="text-destructive h-5 w-5" /> : null}
            {props.variant === "warning" ? <AlertCircleIcon className="h-5 w-5 text-orange-500" /> : null}
          </TooltipTrigger>
          <TooltipContent className="max-w-56">{props.message}</TooltipContent>
        </Tooltip>
      ) : null}
    </div>
  );
}
