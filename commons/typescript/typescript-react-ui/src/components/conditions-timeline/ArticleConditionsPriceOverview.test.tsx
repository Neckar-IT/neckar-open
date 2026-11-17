import type { ArticleConditionsPriceSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ArticleConditionsPriceOverview } from "./ArticleConditionsPriceOverview";

/**
 * Part 20/25 €, accessory 3/4,50 €, half an hour at 75 €/h — the figures of
 * `ArticleConditionsPriceSummaryRequestExample.full`.
 */
const fullSummary: ArticleConditionsPriceSummary = {
  unit: null,
  part: { inputPrice: 2000, sellingPrice: 2500, surcharge: 500, surchargeRate: 0.25 },
  accessory: { inputPrice: 300, sellingPrice: 450, surcharge: 150, surchargeRate: 0.5 },
  labour: { installationTime: "PT30M", hourlyRate: 7500, sellingPrice: 3750 },
  customerPrice: 6700,
};

async function renderExpanded(summary: ArticleConditionsPriceSummary | undefined): Promise<ReturnType<typeof render>> {
  const rendered = render(<ArticleConditionsPriceOverview summary={summary} />);
  await userEvent.setup().click(screen.getByRole("button", { name: /overview/i }));
  return rendered;
}

describe("ArticleConditionsPriceOverview", () => {
  it("starts collapsed behind its title", () => {
    render(<ArticleConditionsPriceOverview summary={fullSummary} />);

    expect(screen.getByRole("button", { name: /overview/i })).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("holds a placeholder until the companion first answered", async () => {
    await renderExpanded(undefined);

    expect(screen.getByText(/conditions overview/i)).toBeInTheDocument();
    expect(screen.getByText(/calculating/i)).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("shows one row per position and the customer price last", async () => {
    const { container } = await renderExpanded(fullSummary);

    const rows = screen.getAllByRole("row").map((row) => row.textContent);
    expect(rows[1]).toMatch(/^Part/);
    expect(rows[2]).toMatch(/^Accessory/);
    expect(rows[3]).toMatch(/^Labour/);
    expect(rows[4]).toMatch(/^Customer price.*67[.,]00\s*€/);
    expect(container).toHaveTextContent(/\+5[.,]00\s*€\s*\(\+25[.,]0\s*%\)/);
    expect(container).toHaveTextContent(/\+1[.,]50\s*€\s*\(\+50[.,]0\s*%\)/);
  });

  it("names installation time, hourly rate and labour price in the labour row", async () => {
    await renderExpanded(fullSummary);

    const labourRow = screen.getAllByRole("row")[3]!;
    expect(labourRow).toHaveTextContent(/0[.,]5\s*h\s*×\s*75[.,]00\s*€\/h/);
    expect(labourRow).toHaveTextContent(/37[.,]50\s*€/);
  });

  it("leaves out the rows the form does not state", async () => {
    await renderExpanded({ ...fullSummary, accessory: null, labour: null, customerPrice: 2500 });

    expect(screen.queryByText(/^Accessory/)).not.toBeInTheDocument();
    expect(screen.queryByText(/^Labour/)).not.toBeInTheDocument();
  });

  it("shows a missing figure as a dash, not as zero", async () => {
    await renderExpanded({
      ...fullSummary,
      part: { inputPrice: 2000, sellingPrice: null, surcharge: null, surchargeRate: null },
      customerPrice: null,
    });

    expect(screen.getAllByRole("row")[4]).toHaveTextContent(/^Customer price\s*-/);
  });

  it("states the unit every amount refers to", async () => {
    await renderExpanded({ ...fullSummary, unit: "Meter" });

    expect(screen.getByText("All amounts per Meter, net.")).toBeInTheDocument();
  });
});
