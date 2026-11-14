/**
 * Imports
 */

import { cn } from "cn";
import { AlertCircleIcon, CheckCircle2Icon, InfoIcon, PlayCircleIcon, StopCircleIcon } from "lucide-react";
import { type TimelineNavStatus } from "./TimelineNavTypes";

/**
 * Prop Types
 */

interface TimelineNavStatusIconProps {
  className?: string | undefined;
  status: TimelineNavStatus;
}

/**
 * Component
 */

export function TimelineNavStatusIcon(props: TimelineNavStatusIconProps): React.ReactElement {
  switch (props.status) {
    case "not-started":
      return <StopCircleIcon className={cn("text-muted-foreground", props.className)} />;
    case "in-progress":
      return <PlayCircleIcon className={cn("text-blue-500", props.className)} />;
    case "completed":
      return <CheckCircle2Icon className={cn("text-green-500", props.className)} />;
    case "warning":
      return <InfoIcon className={cn("text-yellow-500", props.className)} />;
    case "error":
      return <AlertCircleIcon className={cn("text-red-500", props.className)} />;
    default:
      throw new Error(`Unexpected value in Switch Statement: props.status`);
  }
}
