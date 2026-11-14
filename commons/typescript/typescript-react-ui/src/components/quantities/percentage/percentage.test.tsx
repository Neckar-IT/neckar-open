import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import z from "zod";
import { Form } from "../../forms/form";
import { VatRateOutput } from "../vat-rate/vatRate";
import { PercentageInput, PercentageInputFormField, PercentageOutput } from "./percentage";

describe("PercentageOutput", () => {
  it("should render percentage points unconverted", () => {
    render(<PercentageOutput data-testid="percentage-output" value={40} />);
    expect(screen.getByTestId("percentage-output")).toHaveTextContent("40.00 %");
  });

  it("should render zero", () => {
    render(<PercentageOutput data-testid="percentage-output" value={0} />);
    expect(screen.getByTestId("percentage-output")).toHaveTextContent("0.00 %");
  });

  /**
   * The two components render the same unit from different inputs, and confusing them is a factor
   * of 100: PercentageOutput takes percentage points (the shape of the Kotlin `Percentage` value
   * class and therefore the shape on the wire), VatRateOutput takes a fraction.
   */
  it("should read 19 as 19 % where VatRateOutput reads 0.19 as 19 %", () => {
    render(
      <>
        <PercentageOutput data-testid="from-points" value={19} fractionDigits={0} />
        <VatRateOutput data-testid="from-fraction" value={0.19} />
      </>,
    );

    expect(screen.getByTestId("from-points")).toHaveTextContent("19 %");
    expect(screen.getByTestId("from-fraction")).toHaveTextContent("19 %");
  });
});

describe("PercentageInput", () => {
  const TestComponent = ({ initialValue }: { initialValue?: number | undefined }) => {
    const [value, setValue] = useState<number | undefined>(initialValue);

    return (
      <>
        <PercentageInput value={value} onChange={setValue} />
        <div data-testid="value-display">{value}</div>
      </>
    );
  };

  it("should render the initial value unconverted", () => {
    render(<TestComponent initialValue={40} />);

    expect(screen.getByRole("textbox")).toHaveValue("40.00");
    expect(screen.getByTestId("value-display")).toHaveTextContent("40");
  });

  it("should report what the user typed, in percentage points", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    await user.clear(input);
    await user.type(input, "12.34");
    await user.click(valueDisplay); // trigger blur to call onChange

    expect(valueDisplay).toHaveTextContent("12.34");
  });
});

describe("PercentageInputFormField", () => {
  const submit = vi.fn();

  const TestComponent = ({ initialValue }: { initialValue?: number | null }) => {
    const formSchema = z.object({ share: z.number().nullable() });

    const form = useForm({
      resolver: zodResolver(formSchema),
      defaultValues: { share: initialValue ?? null },
    });

    const handleSubmit = form.handleSubmit((data) => {
      submit(data);
    });

    return (
      <Form {...form}>
        <PercentageInputFormField formSchema={formSchema} fieldName="share" />
        <button onClick={handleSubmit}>Submit</button>
      </Form>
    );
  };

  it("should render the initial value unconverted", () => {
    render(<TestComponent initialValue={40} />);
    expect(screen.getByRole("textbox")).toHaveValue("40.00");
  });

  it("should submit percentage points, not a fraction", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");

    await user.clear(input);
    await user.type(input, "23.45");
    await user.click(input); // trigger blur to call onChange
    await user.click(screen.getByText("Submit"));

    expect(submit).toHaveBeenCalledWith({ share: 23.45 });
  });
});
