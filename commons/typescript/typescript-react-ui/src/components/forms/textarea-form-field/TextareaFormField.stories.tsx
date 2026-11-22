/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import * as zod from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { TextareaFormField } from "./TextareaFormField";

const meta = {
  title: "Forms / TextareaFormField",
  component: TextareaFormField,
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  description: string;
  label: string;
  maxLength: number;
  rows: number;
  disabled: boolean;
}

/**
 * Form Schema
 */

const formSchema = zod.object({
  comment: zod.string().trim().nonempty("A comment is required."),
});

type FormSchema = zod.infer<typeof formSchema>;

export const TextareaFormFieldStory: StoryObj<Args> = {
  name: "TextareaFormField",
  args: {
    description: "Please be honest.",
    label: "What is your Opinion?",
    maxLength: 256,
    rows: 3,
    disabled: false,
  },
  argTypes: {
    maxLength: {
      control: {
        type: "number",
      },
    },
    rows: {
      control: {
        type: "number",
      },
    },
  },
  render: (args: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <TextareaFormField {...args} formSchema={formSchema} fieldName="comment" />
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
