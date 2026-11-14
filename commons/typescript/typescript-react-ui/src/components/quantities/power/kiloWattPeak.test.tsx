import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import z from "zod";
import { Form } from "../../forms/form";
import { KiloWattPeakInput, KiloWattPeakInputFormField, KiloWattPeakOutput } from "./kiloWattPeak";

describe("KiloWattPeakOutput", () => {
  it("should render the peak power in kiloWatt correctly", () => {
    render(<KiloWattPeakOutput data-testid="kilo-watt-peak-output" value={5500} />);
    expect(screen.getByTestId("kilo-watt-peak-output")).toHaveTextContent("5.5 kWp");
  });
});

describe("WattPeakInput", () => {
  const TestComponent = ({ initialValue }: { initialValue?: number | undefined }) => {
    const [value, setValue] = useState<number | undefined>(initialValue);

    return (
      <>
        <KiloWattPeakInput value={value} onChange={setValue} />
        <div data-testid="value-display">{value}</div>
      </>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={5500} />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    expect(input).toHaveValue("5.5");
    expect(valueDisplay).toHaveTextContent("5500");
  });

  it("should convert the input value to kiloWatt peak correctly", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    await user.clear(input);
    await user.type(input, "1350");
    await user.click(valueDisplay); // trigger blur to call onChange

    expect(valueDisplay).toHaveTextContent("350");
  });
});

describe("WattPeakInputFormField", () => {
  const submit = vi.fn();

  const TestComponent = ({ initialValue }: { initialValue?: number | null }) => {
    const formSchema = z.object({ peakPower: z.number().nullable() });

    const form = useForm({
      resolver: zodResolver(formSchema),
      defaultValues: { peakPower: initialValue ?? null },
    });

    const handleSubmit = form.handleSubmit((data) => {
      submit(data);
    });

    return (
      <Form {...form}>
        <KiloWattPeakInputFormField formSchema={formSchema} fieldName="peakPower" />
        <button onClick={handleSubmit}>Submit</button>
      </Form>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={10500} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("10.5");
  });

  it("should convert the user input to kilowatt peak on form submission", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const submitButton = screen.getByText("Submit");

    await user.clear(input);
    await user.type(input, "11.49");
    await user.click(input); // trigger blur to call onChange
    await user.click(submitButton);

    expect(submit).toHaveBeenCalledWith({ peakPower: 11490 });
  });
});
