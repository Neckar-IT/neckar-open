/**
 * Imports
 */

import { cn } from "cn";
import { XIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Button } from "../../primitives/button";

/**
 * Prop Types
 */

interface ClearButtonProps {
  onClick: () => void;
  className?: string;
  disabled?: boolean;
}

/**
 * Ghost icon button with an X icon for clearing an input or selection.
 */

export function ClearButton({ onClick, className, disabled }: ClearButtonProps): React.ReactElement | null {
  const { t } = useEmbeddedTranslation();
  return (
    <Button
      className={cn("focus-visible:bg-accent focus-visible:text-accent-foreground h-9.5 w-9.5 focus-visible:ring-0", className)}
      variant="ghost"
      size="icon"
      onClick={onClick}
      tabIndex={-1}
      disabled={disabled}
      aria-label={t("clearButton.label")}
    >
      <XIcon aria-hidden />
    </Button>
  );
}
