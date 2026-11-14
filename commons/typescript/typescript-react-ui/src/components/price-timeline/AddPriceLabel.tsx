import { cn } from "cn";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";

interface AddPriceLabelProps {
  className?: string;
}

/**
 * Empty-state call to action for entities without price data.
 *
 * Renders the localized "add price now" text. Meant to be wrapped in a routing link that points to
 * the price-timeline creation route, replacing the bare NoValue placeholder.
 */
export function AddPriceLabel({ className }: AddPriceLabelProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return <span className={cn("text-primary", className)}>{t("priceSummary.addNow")}</span>;
}
