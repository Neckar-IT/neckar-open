import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form, FormSubmitButton } from "../form";
import { NullableBooleanFormField } from "./NullableBooleanFormField";

const meta = {
  title: "Forms / NullableBooleanFormField",
  component: NullableBooleanFormField,
} satisfies Meta;

export default meta;

const formSchema = z.object({
  sealed: z.boolean().nullable(),
});

type FormSchema = z.infer<typeof formSchema>;

function SealedForm({ sealed }: { sealed: FormSchema["sealed"] }): React.ReactElement {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: { sealed },
  });

  const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
    await delay(500);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-96 flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <NullableBooleanFormField formSchema={formSchema} fieldName="sealed" label="Sealed" description="Empty until someone has looked." />
        <FormSubmitButton className="mt-4">Submit</FormSubmitButton>
      </Form>
      <CodeBlock>{`values: ${JSON.stringify(values)}`}</CodeBlock>
    </div>
  );
}

export const Yes: StoryObj = {
  render: () => <SealedForm sealed={true} />,
};

export const No: StoryObj = {
  render: () => <SealedForm sealed={false} />,
};

export const Empty: StoryObj = {
  render: () => <SealedForm sealed={null} />,
};
