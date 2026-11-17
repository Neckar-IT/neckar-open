/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { Loader2Icon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/**
 * Prop Types
 */

export interface LoadingIndicatorProps {
  className?: string | undefined;
  variant?: "default" | "small" | "icon";
  textKey?: TextKey;
}

/**
 * Animated spinner with optional label shown while data or an action is pending.
 */

export function LoadingIndicator(props: LoadingIndicatorProps) {
  const variant = props.variant ?? "default";

  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults = {
    label: props.textKey ?? "loadingIndicator.label",
  };

  switch (variant) {
    case "default":
      return (
        <div className={cn("text-muted-foreground flex h-full w-full flex-col items-center justify-center gap-2", props.className)}>
          <Loader2Icon className="h-8 w-8 animate-spin" />
          <span className="max-w-56 text-center text-sm">{t(textKeysWithDefaults.label)}</span>
        </div>
      );

    case "small":
      return (
        <div className={cn("text-muted-foreground flex items-center gap-1.5", props.className)}>
          <Loader2Icon className="h-4 w-4 animate-spin" />
          <span className="text-sm whitespace-nowrap">{t(textKeysWithDefaults.label)}</span>
        </div>
      );

    case "icon":
      return <Loader2Icon className={cn("text-muted-foreground size-4 shrink-0 animate-spin", props.className)} />;
    default:
      throw new Error(`Unexpected value in Switch Statement: variant`);
  }
}
