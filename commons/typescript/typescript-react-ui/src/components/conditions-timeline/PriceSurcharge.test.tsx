import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { PriceSurcharge } from "./PriceSurcharge";

describe("PriceSurcharge", () => {
  it("renders profit and rate with an explicit plus for a gain", () => {
    const { container } = render(<PriceSurcharge price={{ inputPrice: 5000, sellingPrice: 6800 }} />);

    expect(container).toHaveTextContent(/\+18[.,]00\s*€\s*\(\+36[.,]0\s*%\)/);
  });

  it("renders a zero surcharge as a value, not as a missing one", () => {
    const { container } = render(<PriceSurcharge price={{ inputPrice: 5000, sellingPrice: 5000 }} />);

    expect(container).toHaveTextContent(/0[.,]00\s*€\s*\(0[.,]0\s*%\)/);
    expect(screen.queryByText("-")).not.toBeInTheDocument();
  });

  it("marks a selling price below the purchase price as destructive", () => {
    const { container } = render(<PriceSurcharge price={{ inputPrice: 5000, sellingPrice: 4000 }} />);

    expect(container).toHaveTextContent(/-10[.,]00\s*€\s*\(-20[.,]0\s*%\)/);
    expect(container.querySelector(".text-destructive")).toBeInTheDocument();
  });

  it("omits the rate for a zero purchase price but keeps the profit", () => {
    const { container } = render(<PriceSurcharge price={{ inputPrice: 0, sellingPrice: 6800 }} />);

    expect(container).toHaveTextContent(/\+68[.,]00\s*€/);
    expect(container).not.toHaveTextContent(/%/);
  });

  it("renders NoValue when the price is explicitly absent", () => {
    render(<PriceSurcharge price={null} />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });
});
