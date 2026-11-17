import type { ArticleConditionsTimelineSummary, ArticleConditionsToDisplay } from "@neckar-hub/typescript-utils/conditions-timeline";
import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ArticleConditionsSummaryHeadline } from "./ArticleConditionsSummaryHeadline";
import { currentConditionsEntry, futureConditionsEntry } from "./conditionsFixtures";

type Summary = ArticleConditionsTimelineSummary<ArticleConditionsToDisplay>;

describe("ArticleConditionsSummaryHeadline", () => {
  it("renders only the current stat when no change is scheduled", () => {
    const summary: Summary = { currentEntry: currentConditionsEntry, nextFutureEntry: null };

    render(<ArticleConditionsSummaryHeadline summary={summary} />);

    expect(screen.getByText(/current conditions/i)).toBeInTheDocument();
    expect(screen.queryByText(/next change/i)).not.toBeInTheDocument();
    expect(screen.getByText(/168[.,]00/)).toBeInTheDocument();
  });

  it("renders both stats when a change is scheduled", () => {
    const summary: Summary = { currentEntry: currentConditionsEntry, nextFutureEntry: futureConditionsEntry };

    const { container } = render(<ArticleConditionsSummaryHeadline summary={summary} />);

    expect(screen.getByText(/current conditions/i)).toBeInTheDocument();
    expect(screen.getByText(/next change/i)).toBeInTheDocument();
    expect(container).toHaveTextContent(/4[.,]382[.,]63\s*€\s*\/ Meter/);
  });

  it("renders the stat values smaller than a single-amount headline", () => {
    const summary: Summary = { currentEntry: currentConditionsEntry, nextFutureEntry: futureConditionsEntry };

    render(<ArticleConditionsSummaryHeadline summary={summary} />);

    expect(screen.getByText(/168[.,]00/).closest("[data-text-content]")).toHaveClass("text-lg");
    expect(screen.getByText(/\/ Meter/)).toHaveClass("text-lg");
  });

  it("renders NoValue in the current stat when there is no current entry", () => {
    const summary: Summary = { currentEntry: null, nextFutureEntry: null };

    render(<ArticleConditionsSummaryHeadline summary={summary} />);

    expect(screen.getByText(/current conditions/i)).toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("renders NoValue when the summary is null", () => {
    render(<ArticleConditionsSummaryHeadline summary={null} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });
});
