import type { PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { PriceSummaryHeadline } from "./PriceSummaryHeadline";

const currentEntry = { price: 1499, validFrom: "2020-01-01T00:00:00Z" };
const futureEntry = { price: 1800, validFrom: "2099-06-01T00:00:00Z" };

describe("PriceSummaryHeadline", () => {
  it("renders the current price and 'since <date>' label", () => {
    const summary: PriceTimelineSummary = { currentEntry, nextFutureEntry: null };

    render(<PriceSummaryHeadline summary={summary} />);

    expect(screen.getByText(/current price/i)).toBeInTheDocument();
    expect(screen.getByText(/14[.,]99/)).toBeInTheDocument();
    expect(screen.getByText(/since/i)).toBeInTheDocument();
  });

  it("renders the next-change line with its price and date when scheduled", () => {
    const summary: PriceTimelineSummary = { currentEntry, nextFutureEntry: futureEntry };

    render(<PriceSummaryHeadline summary={summary} />);

    expect(screen.getByText(/14[.,]99/)).toBeInTheDocument();
    expect(screen.getByText(/18[.,]00/)).toBeInTheDocument();
    expect(screen.getByText(/next change/i)).toBeInTheDocument();
    expect(screen.getByText(/from/i)).toBeInTheDocument();
  });

  it("keeps the headline size of a single amount", () => {
    const summary: PriceTimelineSummary = { currentEntry, nextFutureEntry: futureEntry };

    render(<PriceSummaryHeadline summary={summary} />);

    expect(screen.getByText(/14[.,]99/)).toHaveClass("text-2xl");
    expect(screen.getByText(/18[.,]00/)).toHaveClass("text-2xl");
  });

  it("omits the next-change line when no future entry is scheduled", () => {
    const summary: PriceTimelineSummary = { currentEntry, nextFutureEntry: null };

    render(<PriceSummaryHeadline summary={summary} />);

    expect(screen.queryByText(/next change/i)).not.toBeInTheDocument();
  });

  it("renders NoValue when both entries are null", () => {
    const summary: PriceTimelineSummary = { currentEntry: null, nextFutureEntry: null };

    render(<PriceSummaryHeadline summary={summary} />);

    expect(screen.getByText(/current price/i)).toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("renders NoValue when the summary is null", () => {
    render(<PriceSummaryHeadline summary={null} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });
});
