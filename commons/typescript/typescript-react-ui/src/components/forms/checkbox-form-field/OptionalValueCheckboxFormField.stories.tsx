import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form, FormSubmitButton } from "../form";
import { NumberInputFormField } from "../number-input-form-field";
import { TextInputFormField } from "../text-input-form-field";
import { OptionalValueCheckboxFormField } from "./OptionalValueCheckboxFormField";

const meta = {
  title: "Forms / OptionalValueCheckboxFormField",
  component: OptionalValueCheckboxFormField,
} satisfies Meta;

export default meta;

const formSchema = z.object({
  cashShare: z.number(),
  loan: z.object({ loanAmount: z.number(), bankName: z.string().nullable() }).nullable(),
});

type FormSchema = z.infer<typeof formSchema>;

/**
 * The box is the presence of the loan: checked writes the fresh value, unchecked writes `null`.
 * `loanAmount` has no state a fresh value starts in, so the schema demands it once the box is checked.
 */
export const OptionalValueCheckboxFormFieldStory: StoryObj = {
  render: () => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
      defaultValues: { cashShare: 0, loan: null },
    });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      await delay(500);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-96 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <NumberInputFormField formSchema={formSchema} fieldName="cashShare" label="Cash share" />

          <OptionalValueCheckboxFormField formSchema={formSchema} fieldName="loan" label="Bank loan" presentValue={{ bankName: null }}>
            <NumberInputFormField formSchema={formSchema} fieldName="loan.loanAmount" label="Loan amount" />
            <TextInputFormField formSchema={formSchema} fieldName="loan.bankName" label="Bank" />
          </OptionalValueCheckboxFormField>

          <FormSubmitButton className="mt-4">Submit</FormSubmitButton>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values)}`}</CodeBlock>
      </div>
    );
  },
};
