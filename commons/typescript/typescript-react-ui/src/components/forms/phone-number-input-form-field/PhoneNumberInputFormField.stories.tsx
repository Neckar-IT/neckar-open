/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z as zod } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { PhoneNumberInputFormField } from "./PhoneNumberInputFormField";

const meta = {
  title: "Forms / PhoneNumberInputFormField",
  component: PhoneNumberInputFormField,
} satisfies Meta;

export default meta;

/**
 * Form Schema
 */

const formSchema = zod.object({
  phone: zod.string().min(1),
});

type FormSchema = zod.infer<typeof formSchema>;

export const PhoneNumberInputFormFieldStory: StoryObj = {
  name: "PhoneNumberInputFormField",
  render: () => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (data: FormSchema) => {
      await delay(1000);
      window.alert(JSON.stringify(data, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <PhoneNumberInputFormField
            className="w-80"
            description="Mobile or home phone, it doesn't matter."
            formSchema={formSchema}
            label="Phone"
            fieldName="phone"
          />
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
