import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import z from "zod";
import { Form } from "../../forms/form";
import { MeterInput, MeterInputFormField, MeterOutput } from "./meters";

describe("MeterOutput", () => {
  it("should render the length in meters correctly", () => {
    render(<MeterOutput data-testid="meter-output" value={12.34} />);
    expect(screen.getByTestId("meter-output")).toHaveTextContent("12.34 m");
  });
});

describe("MeterInput", () => {
  const TestComponent = ({ initialValue }: { initialValue?: number | undefined }) => {
    const [value, setValue] = useState<number | undefined>(initialValue);

    return (
      <>
        <MeterInput value={value} onChange={setValue} />
        <div data-testid="value-display">{value}</div>
      </>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={5.67} />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    expect(input).toHaveValue("5.67");
    expect(valueDisplay).toHaveTextContent("5.67");
  });

  it("should convert the input value to meters correctly", async () => {
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

describe("MeterInputFormField", () => {
  const submit = vi.fn();

  const TestComponent = ({ initialValue }: { initialValue?: number | null }) => {
    const formSchema = z.object({ length: z.number().nullable() });

    const form = useForm({
      resolver: zodResolver(formSchema),
      defaultValues: { length: initialValue ?? null },
    });

    const handleSubmit = form.handleSubmit((data) => {
      submit(data);
    });

    return (
      <Form {...form}>
        <MeterInputFormField formSchema={formSchema} fieldName="length" />
        <button onClick={handleSubmit}>Submit</button>
      </Form>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={7.89} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("7.89");
  });

  it("should convert the user input to meters on form submission", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const submitButton = screen.getByText("Submit");

    await user.clear(input);
    await user.type(input, "23.45");
    await user.click(input); // trigger blur to call onChange
    await user.click(submitButton);

    expect(submit).toHaveBeenCalledWith({ length: 23.45 });
  });
});
