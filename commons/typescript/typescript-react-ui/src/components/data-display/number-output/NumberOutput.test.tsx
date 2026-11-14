import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { NumberOutput } from "./NumberOutput";

describe("NumberOutput", () => {
  it("should render the number correctly without unit", () => {
    render(<NumberOutput data-testid="number-display" value={1234.56} />);
    expect(screen.getByTestId("number-display")).toHaveTextContent("1,234.56");
  });

  it("should render the number correctly with unit", () => {
    render(
      <>
        <NumberOutput data-testid="unit-default" value={1234.56} unit="kWh" />
        <NumberOutput data-testid="unit-prefix" value={1234.56} unit="kWh" unitPosition="prefix" />
        <NumberOutput data-testid="unit-suffix" value={1234.56} unit="kWh" unitPosition="suffix" />
      </>,
    );
    expect(screen.getByTestId("unit-default").textContent).toBe("1,234.56 kWh");
    expect(screen.getByTestId("unit-prefix").textContent).toBe("kWh 1,234.56");
    expect(screen.getByTestId("unit-suffix").textContent).toBe("1,234.56 kWh");
  });

  it("should render NoValue when value is null or undefined", () => {
    render(
      <>
        <NumberOutput data-testid="null-value" value={null} />
        <NumberOutput data-testid="undefined-value" value={undefined} />
      </>,
    );
    expect(screen.getByTestId("null-value").textContent).toBe("-");
    expect(screen.getByTestId("undefined-value").textContent).toBe("-");
  });

  it("should respect the fractionDigits prop", () => {
    render(
      <>
        <NumberOutput data-testid="no-fraction" value={1234.56789} fractionDigits={0} />
        <NumberOutput data-testid="two-fractions" value={1234.56789} fractionDigits={2} />
        <NumberOutput data-testid="four-fractions" value={1234.56789} fractionDigits={4} />
      </>,
    );
    expect(screen.getByTestId("no-fraction").textContent).toBe("1,235");
    expect(screen.getByTestId("two-fractions").textContent).toBe("1,234.57");
    expect(screen.getByTestId("four-fractions").textContent).toBe("1,234.5679");
  });

  it("should apply conversion factor correctly", () => {
    render(<NumberOutput data-testid="converted-value" value={123456} conversionFactor={0.001} unit="km" fractionDigits={3} />);
    expect(screen.getByTestId("converted-value").textContent).toBe("123.456 km");
  });
});
