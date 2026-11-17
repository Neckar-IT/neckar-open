import { cn } from "cn";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";

interface IconWithTooltipProps {
  children: React.ReactNode;
  tooltip: React.ReactNode;
  customIconSize?: boolean;
  className?: string;
}

/**
 * Renders the icon (in children) with a tooltip
 */
export function IconWithTooltip({ children, tooltip: title, className }: IconWithTooltipProps): React.ReactElement {
  return (
    <Tooltip>
      <TooltipTrigger asChild className={cn("size-4 focus:outline-none", className)}>
        {children}
      </TooltipTrigger>
      <TooltipContent>{title}</TooltipContent>
    </Tooltip>
  );
}
