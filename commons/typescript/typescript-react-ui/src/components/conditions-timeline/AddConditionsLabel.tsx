import { cn } from "cn";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";

interface AddConditionsLabelProps {
  className?: string;
}

/**
 * Empty-state call to action for entities without conditions data.
 *
 * Renders the localized "add conditions now" text. Meant to be wrapped in a routing link that
 * points to the conditions-timeline creation route, replacing the bare NoValue placeholder.
 */
export function AddConditionsLabel({ className }: AddConditionsLabelProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return <span className={cn("text-primary", className)}>{t("conditionsSummary.addNow")}</span>;
}
