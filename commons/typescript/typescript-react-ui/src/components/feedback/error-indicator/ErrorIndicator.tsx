/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { XCircleIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";

/**
 * Prop Types
 */

export interface ErrorIndicatorProps {
  variant?: "default" | "small" | "icon";
  className?: string | undefined;
  textKey?: TextKey;
}

/**
 * Error state indicator with an X-circle icon and optional label, used when a data fetch or action fails.
 */

export function ErrorIndicator(props: ErrorIndicatorProps) {
  const variant = props.variant ?? "default";

  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults = {
    label: props.textKey ?? "errorIndicator.label",
  };

  switch (variant) {
    case "default":
      return (
        <div className={cn("text-muted-foreground flex h-full w-full flex-col items-center justify-center gap-2", props.className)}>
          <XCircleIcon className="h-8 w-8" />
          <span className="max-w-56 text-center text-sm">{t(textKeysWithDefaults.label)}</span>
        </div>
      );

    case "small":
      return (
        <div className={cn("text-muted-foreground flex items-center justify-center gap-1.5", props.className)}>
          <XCircleIcon className="h-4 w-4" />
          <span className="text-sm whitespace-nowrap">{t(textKeysWithDefaults.label)}</span>
        </div>
      );

    case "icon":
      return props.textKey ? (
        <Tooltip>
          <TooltipTrigger className={cn("text-muted-foreground [&_svg]:size-4 [&_svg]:shrink-0", props.className)}>
            <XCircleIcon />
          </TooltipTrigger>
          <TooltipContent>{t(textKeysWithDefaults.label)}</TooltipContent>
        </Tooltip>
      ) : (
        <XCircleIcon className={cn("text-muted-foreground size-4 shrink-0", props.className)} />
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: variant`);
  }
}
