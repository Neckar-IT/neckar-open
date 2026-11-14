import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import z from "zod";
import { Form } from "../../forms/form";
import { AmpereInput, AmpereInputFormField, AmpereOutput } from "./ampere";

describe("AmpereOUtput", () => {
  it("should render the current in ampere correctly", () => {
    render(<AmpereOutput data-testid="ampere-output" value={5.5} />);
    expect(screen.getByTestId("ampere-output")).toHaveTextContent("5.5 A");
  });
});

describe("AmpereInput", () => {
  const TestComponent = ({ initialValue }: { initialValue?: number | undefined }) => {
    const [value, setValue] = useState<number | undefined>(initialValue);

    return (
      <>
        <AmpereInput value={value} onChange={setValue} />
        <div data-testid="value-display">{value}</div>
      </>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={16} />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    expect(input).toHaveValue("16");
    expect(valueDisplay).toHaveTextContent("16");
  });

  it("should convert the input value to volt correctly", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    await user.clear(input);
    await user.type(input, "20");
    await user.click(valueDisplay); // trigger blur to call onChange

    expect(valueDisplay).toHaveTextContent("20");
  });
});

describe("VoltInputFormField", () => {
  const submit = vi.fn();

  const TestComponent = ({ initialValue }: { initialValue?: number | null }) => {
    const formSchema = z.object({ current: z.number().nullable() });

    const form = useForm({
      resolver: zodResolver(formSchema),
      defaultValues: { current: initialValue ?? null },
    });

    const handleSubmit = form.handleSubmit((data) => {
      submit(data);
    });

    return (
      <Form {...form}>
        <AmpereInputFormField formSchema={formSchema} fieldName="current" />
        <button onClick={handleSubmit}>Submit</button>
      </Form>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={12} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("12");
  });

  it("should convert the user input to watt peak on form submission", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const submitButton = screen.getByText("Submit");

    await user.clear(input);
    await user.type(input, "24.5");
    await user.click(input); // trigger blur to call onChange
    await user.click(submitButton);

    expect(submit).toHaveBeenCalledWith({ current: 24.5 });
  });
});
