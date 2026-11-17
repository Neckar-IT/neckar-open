import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { VatRateOutput } from "./vatRate";

describe("VatRateOutput", () => {
  it("should render the fraction as a percentage", () => {
    render(<VatRateOutput data-testid="vat-rate-output" value={0.19} />);
    expect(screen.getByTestId("vat-rate-output")).toHaveTextContent("19 %");
  });

  it("should render the zero rate", () => {
    render(<VatRateOutput data-testid="vat-rate-output" value={0.0} />);
    expect(screen.getByTestId("vat-rate-output")).toHaveTextContent("0 %");
  });
});
