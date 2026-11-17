import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ArticleConditionsLine } from "./ArticleConditionsLine";
import { conditionsWithAccessory, materialOnlyConditions } from "./conditionsFixtures";

describe("ArticleConditionsLine", () => {
  it("renders the selling price of a per-piece part without a unit suffix", () => {
    render(<ArticleConditionsLine articleConditions={materialOnlyConditions} />);

    expect(screen.getByText(/168[.,]00/)).toBeInTheDocument();
    expect(screen.queryByText(/\//)).not.toBeInTheDocument();
  });

  it("renders the accessory price without a unit of its own and the installation time", () => {
    const { container } = render(<ArticleConditionsLine articleConditions={conditionsWithAccessory} />);

    expect(container).toHaveTextContent(/4[.,]382[.,]63\s*€\s*\/ Meter/);
    expect(container).toHaveTextContent(/210[.,]00\s*€/);
    expect(container).toHaveTextContent(/8[.,]5 h/);
  });

  it("leaves the surcharge out — an overview states what the article sells for", () => {
    render(<ArticleConditionsLine articleConditions={conditionsWithAccessory} />);

    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
  });

  it("omits accessory and installation time when they are absent", () => {
    render(<ArticleConditionsLine articleConditions={materialOnlyConditions} />);

    expect(screen.queryByText(/210[.,]00/)).not.toBeInTheDocument();
    expect(screen.queryByText(/h$/)).not.toBeInTheDocument();
  });
});
