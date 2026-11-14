import { QueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import type { ArticleConditionsPriceSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { act, renderHook, waitFor } from "@testing-library/react";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import {
  articleConditionsTimelineEntryUpsertDefaultValues,
  type ArticleConditionsTimelineEntryUpsertRequestSchema,
} from "./useArticleConditionsTimelineEntryFormResolver";
import {
  type FetchArticleConditionsPriceSummary,
  useConnectArticleConditionsPriceSummaryCompanion,
} from "./useConnectArticleConditionsPriceSummaryCompanion";

const partOnlySummary: ArticleConditionsPriceSummary = {
  unit: null,
  part: { inputPrice: 2000, sellingPrice: 2500, surcharge: 500, surchargeRate: 0.25 },
  accessory: null,
  labour: null,
  customerPrice: 2500,
};

function renderHarness(
  fetchPriceSummary: FetchArticleConditionsPriceSummary,
  template: Parameters<typeof articleConditionsTimelineEntryUpsertDefaultValues>[1] = { conditions: null },
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });

  return renderHook(
    () => {
      const form = useForm<ArticleConditionsTimelineEntryUpsertRequestSchema>({
        defaultValues: articleConditionsTimelineEntryUpsertDefaultValues({ withAccessory: false, withInstallationTime: false }, template),
      });
      const summary = useConnectArticleConditionsPriceSummaryCompanion(form, fetchPriceSummary);
      return { form, summary };
    },
    { wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider> },
  );
}

describe("useConnectArticleConditionsPriceSummaryCompanion", () => {
  it("fetches the overview after a debounced price change and returns it", async () => {
    const fetchPriceSummary = vi.fn<FetchArticleConditionsPriceSummary>().mockResolvedValue({ type: "good", data: partOnlySummary });
    const { result } = renderHarness(fetchPriceSummary);

    act(() => {
      result.current.form.setValue("partPurchasePrice", 2000, { shouldDirty: true });
      result.current.form.setValue("partSellingPrice", 2500, { shouldDirty: true });
    });

    await waitFor(() => {
      expect(fetchPriceSummary).toHaveBeenCalledWith({
        part: { inputPrice: 2000, sellingPrice: 2500 },
        unit: null,
        accessory: null,
        installation: null,
      });
    });
    await waitFor(() => {
      expect(result.current.summary).toEqual(partOnlySummary);
    });
  });

  it("asks for the accessory row once its checkbox is checked", async () => {
    const fetchPriceSummary = vi.fn<FetchArticleConditionsPriceSummary>().mockResolvedValue({ type: "good", data: partOnlySummary });
    const { result } = renderHarness(fetchPriceSummary);

    act(() => {
      result.current.form.setValue("withAccessory", true, { shouldDirty: true });
      result.current.form.setValue("accessoryPurchasePrice", 300, { shouldDirty: true });
    });

    await waitFor(() => {
      expect(fetchPriceSummary).toHaveBeenLastCalledWith(expect.objectContaining({ accessory: { inputPrice: 300, sellingPrice: null } }));
    });
  });

  it("fetches the overview of the conditions the form starts from without an edit", async () => {
    const fetchPriceSummary = vi.fn<FetchArticleConditionsPriceSummary>().mockResolvedValue({ type: "good", data: partOnlySummary });
    const { result } = renderHarness(fetchPriceSummary, { conditions: { part: { inputPrice: 2000, sellingPrice: 2500 } } });

    await waitFor(() => {
      expect(fetchPriceSummary).toHaveBeenCalledWith({
        part: { inputPrice: 2000, sellingPrice: 2500 },
        unit: null,
        accessory: null,
        installation: null,
      });
    });
    await waitFor(() => {
      expect(result.current.summary).toEqual(partOnlySummary);
    });
  });
});
