import { zodResolver } from "@hookform/resolvers/zod";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";
import z from "zod";
import { Form } from "../form";
import { TextInputFormField } from "../text-input-form-field";
import { OptionalValueCheckboxFormField } from "./OptionalValueCheckboxFormField";

const formSchema = z.object({
  loan: z.object({ bankName: z.string().nullable() }).nullable(),
});

type FormSchema = z.infer<typeof formSchema>;

function LoanForm({ loan }: { loan: FormSchema["loan"] }): React.ReactElement {
  const form = useForm<FormSchema>({ resolver: zodResolver(formSchema), defaultValues: { loan } });

  return (
    <Form {...form}>
      <OptionalValueCheckboxFormField formSchema={formSchema} fieldName="loan" label="Loan" presentValue={{ bankName: null }}>
        <TextInputFormField formSchema={formSchema} fieldName="loan.bankName" label="Bank" />
      </OptionalValueCheckboxFormField>
      <output>{JSON.stringify(form.watch("loan"))}</output>
    </Form>
  );
}

describe("OptionalValueCheckboxFormField", () => {
  it("leaves the box unchecked and the fields unmounted while the value is absent", () => {
    render(<LoanForm loan={null} />);

    expect(screen.getByRole("checkbox", { name: "Loan" })).not.toBeChecked();
    expect(screen.queryByLabelText("Bank")).not.toBeInTheDocument();
  });

  it("checks the box and mounts the fields for a value that is there", () => {
    render(<LoanForm loan={{ bankName: "Volksbank" }} />);

    expect(screen.getByRole("checkbox", { name: "Loan" })).toBeChecked();
    expect(screen.getByLabelText("Bank")).toHaveValue("Volksbank");
  });

  it("writes the present value when the box is checked", async () => {
    render(<LoanForm loan={null} />);

    await userEvent.click(screen.getByRole("checkbox", { name: "Loan" }));

    expect(screen.getByRole("status")).toHaveTextContent('{"bankName":null}');
    expect(screen.getByLabelText("Bank")).toBeInTheDocument();
  });

  it("writes nothing at all when the box is unchecked", async () => {
    render(<LoanForm loan={{ bankName: "Volksbank" }} />);

    await userEvent.click(screen.getByRole("checkbox", { name: "Loan" }));

    expect(screen.getByRole("status")).toHaveTextContent("null");
    expect(screen.queryByLabelText("Bank")).not.toBeInTheDocument();
  });
});
