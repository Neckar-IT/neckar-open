import { render, screen } from "@test/test-utils";
import { useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { TextInputFormField } from "../text-input-form-field";
import { Form } from "./Form";

const schema = z.object({ name: z.string() });

function Probe({ disabled }: { disabled: boolean }): React.ReactElement {
  const form = useForm<z.infer<typeof schema>>({ defaultValues: { name: "" }, disabled });
  return (
    <Form {...form}>
      <TextInputFormField formSchema={schema} fieldName="name" label="Name" />
    </Form>
  );
}

describe("FormFieldBase", () => {
  it("disables the field when the whole form is disabled via useForm({ disabled })", () => {
    render(<Probe disabled />);
    expect(screen.getByRole("textbox")).toBeDisabled();
  });

  it("keeps the field enabled when the form is not disabled", () => {
    render(<Probe disabled={false} />);
    expect(screen.getByRole("textbox")).toBeEnabled();
  });

  it("connects the label to the input", () => {
    // The label's `htmlFor` used to address an id no input carried: clicking the caption did not
    // focus the field, and a screen reader announced an unnamed input. `getByLabelText` resolves
    // through exactly that connection, so it fails if the id goes missing again.
    render(<Probe disabled={false} />);
    expect(screen.getByLabelText(/Name/)).toBe(screen.getByRole("textbox"));
  });

  it("gives the input the aria attributes assistive technology reads", () => {
    // `aria-invalid` and the `aria-describedby` tying the input to its description and message come
    // from the same FormControl as the id. Without it a screen reader got none of the three.
    render(<Probe disabled={false} />);
    const input = screen.getByRole("textbox");
    expect(input).toHaveAttribute("aria-describedby");
    expect(input).toHaveAttribute("aria-invalid", "false");
  });
});
