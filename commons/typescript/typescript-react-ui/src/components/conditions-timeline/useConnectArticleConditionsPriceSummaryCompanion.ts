import type {
  ArticleConditionsPriceSummary,
  ArticleConditionsPriceSummaryRequest,
  ArticleConditionsPriceSummaryResponse,
} from "@neckar-hub/typescript-utils/openapi-types";
import { useState } from "react";
import { type UseFormReturn, useWatch } from "react-hook-form";
import { useConnectCompanion } from "../../query-handling/useConnectCompanion";
import { toArticleConditionsPriceSummaryRequest } from "./toArticleConditionsPriceSummaryRequest";
import type { ArticleConditionsTimelineEntryUpsertRequestSchema } from "./useArticleConditionsTimelineEntryFormResolver";

/**
 * The generated client call of the product's price-overview companion — every product generates it under the
 * same name, `calculateArticleConditionsPriceSummary`.
 */
export type FetchArticleConditionsPriceSummary = (
  request: ArticleConditionsPriceSummaryRequest,
) => Promise<ArticleConditionsPriceSummaryResponse>;

/**
 * Connects the add-conditions form to its price-overview companion: watches every field the overview
 * reads (prices, unit, hours and both checkboxes), fetches the overview debounced, and returns the
 * latest answer — `undefined` until the first one.
 *
 * Runs from the first render, not only once the form is dirty: the overview is shown beside the form and
 * has no value of its own until it is fetched, also for the conditions a new entry starts from.
 */
export function useConnectArticleConditionsPriceSummaryCompanion(
  form: UseFormReturn<ArticleConditionsTimelineEntryUpsertRequestSchema>,
  fetchPriceSummary: FetchArticleConditionsPriceSummary,
): ArticleConditionsPriceSummary | undefined {
  const [
    partPurchasePrice,
    partSellingPrice,
    partUnit,
    withAccessory,
    accessoryPurchasePrice,
    accessorySellingPrice,
    withInstallationTime,
    installationTimeHours,
  ] = useWatch({
    control: form.control,
    name: [
      "partPurchasePrice",
      "partSellingPrice",
      "partUnit",
      "withAccessory",
      "accessoryPurchasePrice",
      "accessorySellingPrice",
      "withInstallationTime",
      "installationTimeHours",
    ],
  });
  const [summary, setSummary] = useState<ArticleConditionsPriceSummary>();

  useConnectCompanion<ArticleConditionsPriceSummaryRequest, ArticleConditionsPriceSummary>({
    input: toArticleConditionsPriceSummaryRequest({
      partPurchasePrice,
      partSellingPrice,
      partUnit,
      withAccessory,
      accessoryPurchasePrice,
      accessorySellingPrice,
      withInstallationTime,
      installationTimeHours,
    }),
    inputEquals: (left, right) => JSON.stringify(left) === JSON.stringify(right),
    enabled: true,
    queryKey: (input) => ["conditions-timeline", "companion", "price-summary", input],
    fetchValue: async (input) => {
      const response = await fetchPriceSummary(input);
      // A bad response is ignored: the overview keeps its last answer instead of showing a failure.
      if (response.type !== "good") return undefined;
      return response.data;
    },
    applyValue: setSummary,
    // The hourly rate is server state the input does not carry; a cached answer could price labour
    // at an outdated rate.
    staleTimeMs: 0,
  });

  return summary;
}
