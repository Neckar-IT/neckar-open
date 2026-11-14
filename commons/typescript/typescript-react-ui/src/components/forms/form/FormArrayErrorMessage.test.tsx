import { zodResolver } from "@hookform/resolvers/zod";
import { fireEvent, render, screen, waitFor } from "@test/test-utils";
import { useFieldArray, useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { Button } from "../../primitives/button";
import { TextInputFormField } from "../text-input-form-field";
import { Form } from "./Form";
import { FormArrayErrorMessage } from "./FormArrayErrorMessage";

const formSchema = z.object({
  ingredients: z
    .array(
      z.object({
        name: z.string().nonempty("Name must not be empty"),
      }),
    )
    .min(1, "Add at least one ingredient"),
});

type FormSchema = z.infer<typeof formSchema>;

function Harness({ initialRows }: { initialRows: FormSchema["ingredients"] }): React.ReactElement {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: { ingredients: initialRows },
  });
  const { fields } = useFieldArray({ control: form.control, name: "ingredients" });

  return (
    <Form {...form} onSubmit={form.handleSubmit(() => {})}>
      {fields.map((field, index) => (
        <TextInputFormField key={field.id} formSchema={formSchema} fieldName={`ingredients.${index}.name`} label="Name" />
      ))}
      <FormArrayErrorMessage formSchema={formSchema} fieldName="ingredients" />
      <Button type="submit">Submit</Button>
    </Form>
  );
}

describe("FormArrayErrorMessage", () => {
  it("renders the list-level error of the array field", async () => {
    render(<Harness initialRows={[]} />);
    expect(screen.queryByText("Add at least one ingredient")).toBeNull();

    fireEvent.click(screen.getByText("Submit"));

    await waitFor(() => expect(screen.getByText("Add at least one ingredient")).toBeTruthy());
  });

  it("renders nothing for row errors — those belong to the row's own form field", async () => {
    render(<Harness initialRows={[{ name: "" }]} />);

    fireEvent.click(screen.getByText("Submit"));

    await waitFor(() => expect(screen.getByText("Name must not be empty")).toBeTruthy());
    expect(screen.queryByText("Add at least one ingredient")).toBeNull();
  });
});
