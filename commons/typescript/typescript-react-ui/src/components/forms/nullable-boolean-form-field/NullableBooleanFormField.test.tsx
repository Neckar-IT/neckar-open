import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";
import z from "zod";
import { Form } from "../form";
import { NullableBooleanFormField } from "./NullableBooleanFormField";

const formSchema = z.object({
  sealed: z.boolean().nullable(),
});

type FormSchema = z.infer<typeof formSchema>;

function SealedForm({ sealed }: { sealed?: FormSchema["sealed"] }): React.ReactElement {
  const form = useForm<FormSchema>({ resolver: zodResolver(formSchema), defaultValues: { sealed } });

  return (
    <Form {...form}>
      <NullableBooleanFormField formSchema={formSchema} fieldName="sealed" label="Sealed" />
      <output>{JSON.stringify(form.watch("sealed"))}</output>
    </Form>
  );
}

function item(name: "Yes" | "No" | "Not specified"): HTMLElement {
  return screen.getByRole("radio", { name });
}

describe("NullableBooleanFormField", () => {
  it.each([
    [true, "Yes"],
    [false, "No"],
    [null, "Not specified"],
  ] as const)("presses the item of the start value %s", (sealed, pressed) => {
    render(<SealedForm sealed={sealed} />);

    expect(item(pressed)).toHaveAttribute("aria-checked", "true");
    expect(screen.getAllByRole("radio").filter((radio) => radio.getAttribute("aria-checked") === "true")).toHaveLength(1);
  });

  it("reaches yes, empty and no one after another", async () => {
    render(<SealedForm sealed={null} />);

    await userEvent.click(item("Yes"));
    expect(screen.getByRole("status")).toHaveTextContent("true");

    await userEvent.click(item("Not specified"));
    expect(screen.getByRole("status")).toHaveTextContent("null");

    await userEvent.click(item("No"));
    expect(screen.getByRole("status")).toHaveTextContent("false");
  });

  it("presses no item without a start value and writes null on a click on empty", async () => {
    render(<SealedForm />);

    expect(screen.getAllByRole("radio").filter((radio) => radio.getAttribute("aria-checked") === "true")).toHaveLength(0);

    await userEvent.click(item("Not specified"));
    expect(screen.getByRole("status")).toHaveTextContent("null");
  });

  it("keeps the value on a click on the pressed item", async () => {
    render(<SealedForm sealed={true} />);

    await userEvent.click(item("Yes"));

    expect(screen.getByRole("status")).toHaveTextContent("true");
    expect(item("Yes")).toHaveAttribute("aria-checked", "true");
  });
});
