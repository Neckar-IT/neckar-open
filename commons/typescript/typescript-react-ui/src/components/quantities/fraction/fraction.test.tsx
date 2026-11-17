import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { describe, expect, it, vi } from "vitest";
import z from "zod";
import { Form } from "../../forms/form";
import { FractionInputFormField, FractionOutput } from "./fraction";

describe("FractionOutput", () => {
  it("should render a fraction in percent", () => {
    render(<FractionOutput data-testid="fraction-output" value={0.05} />);
    expect(screen.getByTestId("fraction-output")).toHaveTextContent("5.00 %");
  });
});

describe("FractionInputFormField", () => {
  const submit = vi.fn();

  const TestComponent = ({ initialValue }: { initialValue?: number | null }) => {
    const formSchema = z.object({ discount: z.number().nullable() });
    const form = useForm({
      resolver: zodResolver(formSchema),
      defaultValues: { discount: initialValue ?? null },
    });
    const handleSubmit = form.handleSubmit((data) => {
      submit(data);
    });

    return (
      <Form {...form}>
        <FractionInputFormField formSchema={formSchema} fieldName="discount" />
        <button onClick={handleSubmit}>Submit</button>
      </Form>
    );
  };

  it("should render the fraction in percent", () => {
    render(<TestComponent initialValue={0.05} />);
    expect(screen.getByRole("textbox")).toHaveValue("5.00");
  });

  it("should submit the fraction the typed percent stands for", async () => {
    const user = userEvent.setup();
    render(<TestComponent />);
    const input = screen.getByRole("textbox");

    await user.clear(input);
    await user.type(input, "12.5");
    await user.click(input);
    await user.click(screen.getByText("Submit"));

    expect(submit).toHaveBeenCalledWith({ discount: 0.125 });
  });

  it("should move the fraction by one percentage point on an arrow key", async () => {
    const user = userEvent.setup();
    render(<TestComponent initialValue={0.25} />);
    const input = screen.getByRole("textbox");

    await user.click(input);
    await user.keyboard("{ArrowUp}");
    await user.click(screen.getByText("Submit"));

    expect(input).toHaveValue("26.00");
    expect(submit).toHaveBeenLastCalledWith({ discount: 0.26 });
  });
});
