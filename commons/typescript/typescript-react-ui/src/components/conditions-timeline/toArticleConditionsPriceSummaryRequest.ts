import { formatIsoDurationFromHours } from "@neckar-hub/typescript-utils/duration";
import type { ArticleConditionsPriceSummaryRequest } from "@neckar-hub/typescript-utils/openapi-types";
import { normalizePartUnit } from "./toArticleConditions";
import type { ArticleConditionsTimelineEntryUpsertRequestSchema } from "./useArticleConditionsTimelineEntryFormResolver";

/** The form values the price overview reads — partial, because an untouched price field holds `undefined`. */
export type ArticleConditionsPriceSummaryFormValues = Partial<
  Pick<
    ArticleConditionsTimelineEntryUpsertRequestSchema,
    | "partPurchasePrice"
    | "partSellingPrice"
    | "partUnit"
    | "withAccessory"
    | "accessoryPurchasePrice"
    | "accessorySellingPrice"
    | "withInstallationTime"
    | "installationTimeHours"
  >
>;

/**
 * Maps the flat form values onto the price-overview companion's request — field mapping only, the
 * arithmetic stays on the server. An unchecked checkbox drops its row; an empty field is sent as `null`.
 */
export function toArticleConditionsPriceSummaryRequest(
  values: ArticleConditionsPriceSummaryFormValues,
): ArticleConditionsPriceSummaryRequest {
  return {
    part: { inputPrice: values.partPurchasePrice ?? null, sellingPrice: values.partSellingPrice ?? null },
    unit: normalizePartUnit(values.partUnit),
    accessory: values.withAccessory
      ? { inputPrice: values.accessoryPurchasePrice ?? null, sellingPrice: values.accessorySellingPrice ?? null }
      : null,
    installation: values.withInstallationTime
      ? { installationTime: values.installationTimeHours == null ? null : formatIsoDurationFromHours(values.installationTimeHours) }
      : null,
  };
}
