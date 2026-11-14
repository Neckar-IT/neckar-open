import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import z from "zod";
import { Form } from "../../forms/form";
import { EuroInput, EuroInputFormField, EuroOutput } from "./euros";

describe("EuroOutput", () => {
  it("should render the price in euros correctly", () => {
    render(<EuroOutput data-testid="euro-output" value={4270} />);
    expect(screen.getByTestId("euro-output")).toHaveTextContent("42.70 €");
  });
});

describe("EuroInput", () => {
  const TestComponent = ({ initialValue }: { initialValue?: number | undefined }) => {
    const [value, setValue] = useState<number | undefined>(initialValue);

    return (
      <>
        <EuroInput value={value} onChange={setValue} />
        <div data-testid="value-display">{value}</div>
      </>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={567} />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    expect(input).toHaveValue("5.67");
    expect(valueDisplay).toHaveTextContent("567");
  });

  it("should convert the input value to euros correctly", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const valueDisplay = screen.getByTestId("value-display");

    await user.clear(input);
    await user.type(input, "12.34");
    await user.click(valueDisplay); // trigger blur to call onChange

    expect(valueDisplay).toHaveTextContent("1234");
  });
});

describe("EuroInputFormField", () => {
  const submit = vi.fn();

  const TestComponent = ({ initialValue }: { initialValue?: number | null }) => {
    const formSchema = z.object({ price: z.number().nullable() });

    const form = useForm({
      resolver: zodResolver(formSchema),
      defaultValues: { price: initialValue ?? null },
    });

    const handleSubmit = form.handleSubmit((data) => {
      submit(data);
    });

    return (
      <Form {...form}>
        <EuroInputFormField formSchema={formSchema} fieldName="price" />
        <button onClick={handleSubmit}>Submit</button>
      </Form>
    );
  };

  it("should render the initial value correctly", () => {
    render(<TestComponent initialValue={789} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveValue("7.89");
  });

  it("should convert the user input to euros on form submission", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);

    const input = screen.getByRole("textbox");
    const submitButton = screen.getByText("Submit");

    await user.clear(input);
    await user.type(input, "23.45");
    await user.click(input); // trigger blur to call onChange
    await user.click(submitButton);

    expect(submit).toHaveBeenCalledWith({ price: 2345 });
  });
});
