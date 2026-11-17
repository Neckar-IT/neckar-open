import type { PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { PriceSummary } from "./PriceSummary";

const currentEntry = { price: 1499, validFrom: "2020-01-01T00:00:00Z" };
const futureEntry = { price: 1800, validFrom: "2099-06-01T00:00:00Z" };

describe("PriceSummary (full)", () => {
  it("renders current price and 'since <date>' when only currentEntry is present", () => {
    const summary: PriceTimelineSummary = { currentEntry, nextFutureEntry: null };

    render(<PriceSummary variant="full" summary={summary} />);

    expect(screen.getByText(/14[.,]99/)).toBeInTheDocument();
    expect(screen.getByText(/since/i)).toBeInTheDocument();
    expect(screen.queryByText(/from/i)).not.toBeInTheDocument();
  });

  it("renders both current and next entries when both are present", () => {
    const summary: PriceTimelineSummary = { currentEntry, nextFutureEntry: futureEntry };

    render(<PriceSummary variant="full" summary={summary} />);

    expect(screen.getByText(/14[.,]99/)).toBeInTheDocument();
    expect(screen.getByText(/18[.,]00/)).toBeInTheDocument();
    expect(screen.getByText(/since/i)).toBeInTheDocument();
    expect(screen.getByText(/from/i)).toBeInTheDocument();
  });

  it("renders NoValue when both currentEntry and nextFutureEntry are null", () => {
    const summary: PriceTimelineSummary = { currentEntry: null, nextFutureEntry: null };

    render(<PriceSummary variant="full" summary={summary} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("renders NoValue when the summary is null", () => {
    render(<PriceSummary variant="full" summary={null} />);

    expect(screen.getByText("-")).toBeInTheDocument();
    expect(screen.queryByText(/14[.,]99/)).not.toBeInTheDocument();
  });
});

describe("PriceSummary (compact)", () => {
  it("renders the current price from the summary's currentEntry", () => {
    const summary: PriceTimelineSummary = { currentEntry, nextFutureEntry: null };

    render(<PriceSummary variant="compact" summary={summary} />);

    expect(screen.getByText(/14[.,]99/)).toBeInTheDocument();
    expect(screen.queryByText(/since/i)).not.toBeInTheDocument();
  });

  it("renders NoValue when currentEntry is null", () => {
    const summary: PriceTimelineSummary = { currentEntry: null, nextFutureEntry: futureEntry };

    render(<PriceSummary variant="compact" summary={summary} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("renders NoValue when the summary is null", () => {
    render(<PriceSummary variant="compact" summary={null} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });
});

describe("PriceSummary euro formatting", () => {
  it("renders cents as a formatted euro amount", () => {
    const summary: PriceTimelineSummary = {
      currentEntry: { price: 12345, validFrom: "2020-01-01T00:00:00Z" },
      nextFutureEntry: null,
    };

    render(<PriceSummary variant="compact" summary={summary} />);

    const formatted = screen.getByText(/123[.,]45/);
    expect(formatted).toHaveTextContent(/€|EUR/);
  });
});
