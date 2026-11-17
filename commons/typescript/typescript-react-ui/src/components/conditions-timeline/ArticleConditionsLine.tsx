import type { ArticleConditionsToDisplay } from "@neckar-hub/typescript-utils/conditions-timeline";
import { cn } from "cn";
import { ArticleConditionsAccessoryPrice } from "./ArticleConditionsAccessoryPrice";
import { ArticleConditionsInstallationTime } from "./ArticleConditionsInstallationTime";
import { ArticleConditionsSellingPrice } from "./ArticleConditionsSellingPrice";

interface ArticleConditionsLineProps {
  articleConditions: ArticleConditionsToDisplay;
  /**
   * Class names for the leading selling price, so a summary can render it at stat size while the
   * accessory and the installation time stay subordinate.
   */
  priceClassName?: string;
  className?: string;
}

/**
 * Condensed one-line rendering of the commercial state of an article: the selling price per unit,
 * followed by what the accessory adds to that unit and the installation time, when they are set.
 *
 * Deliberately without the surcharge: it only reads correctly next to a label ({@link PriceSurcharge}
 * says why), and this line has no room for one. The surcharge lives beside the purchase price in the cells of the
 * conditions table and in the price overview at the end of the entry form.
 *
 * Used wherever a whole conditions record has to fit on one line — the summary stats, the inline
 * summary, table cells that show "the conditions" rather than a single field. Renders every optional
 * part an article type has and skips the ones it does not, so it takes
 * {@link ArticleConditionsToDisplay} rather than one shape.
 */
export function ArticleConditionsLine({ articleConditions, priceClassName, className }: ArticleConditionsLineProps): React.ReactElement {
  return (
    <span className={cn("flex min-w-0 flex-wrap items-baseline gap-x-2", className)}>
      <ArticleConditionsSellingPrice articleConditions={articleConditions} className={priceClassName} />

      {articleConditions.accessory != null ? (
        <span className="text-muted-foreground text-xs">
          + <ArticleConditionsAccessoryPrice accessory={articleConditions.accessory} />
        </span>
      ) : null}

      {articleConditions.installationTime != null ? (
        <span className="text-muted-foreground text-xs">
          · <ArticleConditionsInstallationTime articleConditions={articleConditions} />
        </span>
      ) : null}
    </span>
  );
}
