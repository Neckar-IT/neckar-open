import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { AddPriceLabel } from "./AddPriceLabel";

describe("AddPriceLabel", () => {
  it("renders the localized add-now call to action", () => {
    render(<AddPriceLabel />);

    expect(screen.getByText(/add now/i)).toBeInTheDocument();
  });

  it("merges a custom className", () => {
    render(<AddPriceLabel className="custom-class" />);

    expect(screen.getByText(/add now/i)).toHaveClass("custom-class");
  });
});
