import type { ArticleConditionsTimelineSummary, ArticleConditionsToDisplay } from "@neckar-hub/typescript-utils/conditions-timeline";
import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ArticleConditionsSummary } from "./ArticleConditionsSummary";
import { currentConditionsEntry, futureConditionsEntry } from "./conditionsFixtures";

type Summary = ArticleConditionsTimelineSummary<ArticleConditionsToDisplay>;

describe("ArticleConditionsSummary (full)", () => {
  it("renders the current conditions without any date when only currentEntry is present", () => {
    const summary: Summary = { currentEntry: currentConditionsEntry, nextFutureEntry: null };

    render(<ArticleConditionsSummary variant="full" summary={summary} />);

    expect(screen.getByText(/168[.,]00/)).toBeInTheDocument();
    expect(screen.queryByText(/since/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/from/i)).not.toBeInTheDocument();
  });

  it("renders both current and next entries when both are present, dating only the next entry", () => {
    const summary: Summary = { currentEntry: currentConditionsEntry, nextFutureEntry: futureConditionsEntry };

    const { container } = render(<ArticleConditionsSummary variant="full" summary={summary} />);

    expect(container).toHaveTextContent(/168[.,]00\s*€/);
    expect(container).toHaveTextContent(/4[.,]382[.,]63\s*€\s*\/ Meter/);
    expect(screen.queryByText(/since/i)).not.toBeInTheDocument();
    expect(screen.getByText(/from/i)).toBeInTheDocument();
  });

  it("renders the current conditions one type step below the body size", () => {
    const summary: Summary = { currentEntry: currentConditionsEntry, nextFutureEntry: futureConditionsEntry };

    render(<ArticleConditionsSummary variant="full" summary={summary} />);

    expect(screen.getByText(/168[.,]00/).closest("[data-text-content]")).toHaveClass("text-sm");
    expect(screen.getByText(/\/ Meter/)).toHaveClass("text-xs");
  });

  it("renders NoValue when both entries are null", () => {
    const summary: Summary = { currentEntry: null, nextFutureEntry: null };

    render(<ArticleConditionsSummary variant="full" summary={summary} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("renders NoValue when the summary is null", () => {
    render(<ArticleConditionsSummary variant="full" summary={null} />);

    expect(screen.getByText("-")).toBeInTheDocument();
    expect(screen.queryByText(/168[.,]00/)).not.toBeInTheDocument();
  });
});

describe("ArticleConditionsSummary (compact)", () => {
  it("renders only the current conditions", () => {
    const summary: Summary = { currentEntry: currentConditionsEntry, nextFutureEntry: futureConditionsEntry };

    render(<ArticleConditionsSummary variant="compact" summary={summary} />);

    expect(screen.getByText(/168[.,]00/)).toBeInTheDocument();
    expect(screen.queryByText(/4[.,]382[.,]63/)).not.toBeInTheDocument();
    expect(screen.queryByText(/since/i)).not.toBeInTheDocument();
  });

  it("renders NoValue when there is no current entry", () => {
    const summary: Summary = { currentEntry: null, nextFutureEntry: futureConditionsEntry };

    render(<ArticleConditionsSummary variant="compact" summary={summary} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });
});
