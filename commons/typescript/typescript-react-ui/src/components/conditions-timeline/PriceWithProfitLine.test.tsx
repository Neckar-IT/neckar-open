import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { EuroOutput } from "../quantities/money/euros";
import { PriceWithProfitLine } from "./PriceWithProfitLine";

/** Renders one amount the way a per-piece record would — the simplest `renderAmount` there is. */
function euros(value: number): React.ReactNode {
  return <EuroOutput value={value} />;
}

describe("PriceWithProfitLine", () => {
  it("shows both amounts through the caller's renderer and the surcharge between them", () => {
    const { container } = render(<PriceWithProfitLine price={{ inputPrice: 130, sellingPrice: 182 }} renderAmount={euros} />);

    expect(container).toHaveTextContent(/1[.,]30\s*€\s*→\s*1[.,]82\s*€\s*\(\+40[.,]0\s*%\)/);
  });

  it("states the rate and not the absolute profit — that one is the difference already on the line", () => {
    render(<PriceWithProfitLine price={{ inputPrice: 130, sellingPrice: 182 }} renderAmount={euros} />);

    expect(screen.queryByText(/0[.,]52/)).not.toBeInTheDocument();
  });

  it("renders selling below cost as a negative rate", () => {
    const { container } = render(<PriceWithProfitLine price={{ inputPrice: 200, sellingPrice: 150 }} renderAmount={euros} />);

    expect(container).toHaveTextContent(/\(-25[.,]0\s*%\)/);
  });

  it("shows the amounts alone when the purchase price is zero — a surcharge on nothing is undefined", () => {
    const { container } = render(<PriceWithProfitLine price={{ inputPrice: 0, sellingPrice: 500 }} renderAmount={euros} />);

    expect(container).toHaveTextContent(/0[.,]00\s*€\s*→\s*5[.,]00\s*€/);
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
  });

  it("renders selling at cost as a value rather than as no value", () => {
    const { container } = render(<PriceWithProfitLine price={{ inputPrice: 130, sellingPrice: 130 }} renderAmount={euros} />);

    expect(container).toHaveTextContent(/\(0[.,]0\s*%\)/);
  });
});
