import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form, FormSubmitButton } from "../form";
import { CheckboxFormField } from "./CheckboxFormField";

const meta = {
  title: "Forms / CheckboxFormField",
} satisfies Meta;

export default meta;

interface Args {
  label: string;
  description: string;
  disabled: boolean;
}

const formSchema = z.object({
  agree: z.boolean(),
});

type FormSchema = z.infer<typeof formSchema>;

/**
 * Stories
 */

export const CheckboxFormFieldStory: StoryObj<Args> = {
  name: "CheckboxFormField",
  args: {
    label: "I agree to the terms and conditions",
    description: "Our terms and conditions apply always and everywhere.",
    disabled: false,
  },
  render: (args: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      await delay(500);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-96 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <CheckboxFormField {...args} formSchema={formSchema} fieldName="agree" />
          <FormSubmitButton className="mt-4">Submit</FormSubmitButton>
        </Form>
        <CodeBlock>{`values: ${JSON.stringify(values)}`}</CodeBlock>
      </div>
    );
  },
};
