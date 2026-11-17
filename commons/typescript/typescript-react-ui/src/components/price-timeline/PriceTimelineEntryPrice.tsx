import { useCurrencyFormatter } from "@neckar-hub/typescript-react-i18n";
import type { PriceTimelineEntryView } from "@neckar-hub/typescript-utils/openapi-types";
import { entityPropertyRenderer } from "../data-display/entity-property-renderer";

/**
 * Displays the price of a price timeline entry, formatted as euro currency.
 * Price is stored in cents on the wire.
 */
export const PriceTimelineEntryPrice = entityPropertyRenderer("priceTimelineEntry")<PriceTimelineEntryView>({
  displayName: "PriceTimelineEntryPrice",
  render: ({ entity }) => {
    const formatCurrency = useCurrencyFormatter();
    return formatCurrency(entity.price / 100);
  },
});
