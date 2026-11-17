/**
 * Imports
 */

import type { TextKey, TextKeyWithOptions } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { AlertCircleIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Tooltip, TooltipContent, TooltipTrigger } from "../../primitives/tooltip";
import { Empty } from "../empty";

/**
 * Prop Types
 */

interface NotFoundIndicatorProps {
  className?: string;
  variant?: "default" | "small" | "icon";
  labelComponent?: React.ReactNode;
  textKey?: TextKey | TextKeyWithOptions;
  /** Rendered below the label by the `default` variant, e.g. the action that fills the empty list. */
  children?: React.ReactNode;
}

/**
 * Component to display when no items were found
 */

export function NotFoundIndicator(props: NotFoundIndicatorProps) {
  const variant = props.variant ?? "default";

  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults = {
    label: props.textKey ?? "notFoundIndicator.label",
  };

  switch (variant) {
    case "default":
      return (
        <div className={cn("flex h-full w-full flex-col items-center justify-center gap-2", props.className)}>
          <Empty className="h-16 w-16" />
          {props.labelComponent ?? (
            <span className="text-muted-foreground max-w-56 text-center text-sm">{t(textKeysWithDefaults.label)}</span>
          )}
          {props.children}
        </div>
      );

    case "small":
      return (
        <div className={cn("text-muted-foreground flex items-center gap-1.5", props.className)}>
          <AlertCircleIcon className="h-4 w-4" />
          <span className="truncate text-sm whitespace-nowrap">{t(textKeysWithDefaults.label)}</span>
        </div>
      );

    case "icon":
      return props.textKey !== undefined && props.textKey !== "" ? (
        <Tooltip>
          <TooltipTrigger className={cn("text-muted-foreground [&_svg]:size-4 [&_svg]:shrink-0", props.className)}>
            <AlertCircleIcon />
          </TooltipTrigger>
          <TooltipContent>{t(textKeysWithDefaults.label)}</TooltipContent>
        </Tooltip>
      ) : (
        <AlertCircleIcon className={cn("text-muted-foreground size-4 shrink-0", props.className)} />
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: variant`);
  }
}
