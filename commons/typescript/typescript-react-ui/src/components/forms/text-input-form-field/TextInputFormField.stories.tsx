/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { TextInputFormField } from "./TextInputFormField";

/**
 * Meta Information
 */

const meta = {
  title: "Forms / TextInputFormField",
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  label: string;
  description: string;
  placeholder: string;
  maxLength: number;
  disabled: boolean;
}

/**
 * Form Schema
 */

const formSchema = z.object({
  name: z.string().nonempty(),
});

type FormSchema = z.infer<typeof formSchema>;

/**
 * Stories
 */

export const TextInputFormFieldStory: StoryObj<Args> = {
  name: "TextInputFormField",
  args: {
    label: "What is your Name?",
    description: "First and last name.",
    placeholder: "John Doe",
    maxLength: 64,
    disabled: false,
  },
  render: (args: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const name = form.watch("name");

    return (
      <div className="flex w-96 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <TextInputFormField {...args} formSchema={formSchema} fieldName="name" />
          <Button className="mt-4" loading={form.formState.isSubmitting} type="submit">
            Submit
          </Button>

          <CodeBlock>{`value: ${JSON.stringify(name)}`}</CodeBlock>
        </Form>
      </div>
    );
  },
};
