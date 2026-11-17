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
import { DurationInputFormField } from "./DurationInputFormField";

/**
 * Meta Information
 */

const meta = {
  title: "Forms / DurationInputFormField",
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  description: string;
  label: string;
  step: number;
  disabled: boolean;
}

/**
 * Form Schema
 */

const formSchema = zod.object({
  duration: zod.number().int().max(90000, "Duration cannot be longer than 90 seconds."),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const DurationInputFormFieldStory: StoryObj<Args> = {
  name: "DurationInputFormField",
  args: {
    description: "The duration in seconds.",
    label: "How long is the duration?",
    step: 5000,
    disabled: false,
  },
  render: (args: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
      defaultValues: {
        duration: 0,
      },
    });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <DurationInputFormField {...args} formSchema={formSchema} fieldName="duration" />
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
