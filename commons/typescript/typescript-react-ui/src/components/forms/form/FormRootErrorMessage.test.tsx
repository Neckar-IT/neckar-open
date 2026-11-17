import { zodResolver } from "@hookform/resolvers/zod";
import { fireEvent, render, screen, waitFor } from "@test/test-utils";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { ButtonBar } from "../../layout/button-bar";
import { Button } from "../../primitives/button";
import { TextInputFormField } from "../text-input-form-field";
import { FormCard, FormCardContent, FormCardFooter } from "./FormCard";

const rootMessage = "Die drei Arbeitsschritte ergeben zusammen nicht 100 %.";

const formSchema = z.object({
  name: z.string().nonempty("Name must not be empty"),
});

type FormSchema = z.infer<typeof formSchema>;

function Harness({ name, rootError }: { name: string; rootError: boolean }): React.ReactElement {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: { name },
  });

  useEffect(() => {
    if (rootError) {
      form.setError("root", { type: "server", message: rootMessage });
    }
  }, [form, rootError]);

  return (
    <FormCard form={form} onSubmit={form.handleSubmit(() => {})}>
      <FormCardContent>
        <TextInputFormField formSchema={formSchema} fieldName="name" label="Name" />
      </FormCardContent>
      <FormCardFooter>
        <ButtonBar>
          <Button type="submit">Submit</Button>
        </ButtonBar>
      </FormCardFooter>
    </FormCard>
  );
}

describe("FormRootErrorMessage", () => {
  it("renders the root error above the submit button of a FormCard", async () => {
    render(<Harness name="Montage" rootError={true} />);

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toBe(rootMessage);
    const submit = screen.getByRole("button", { name: "Submit" });
    expect(alert.compareDocumentPosition(submit) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("renders no alert for a field error without a root error", async () => {
    render(<Harness name="" rootError={false} />);

    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => expect(screen.getByText("Name must not be empty")).toBeTruthy());
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
