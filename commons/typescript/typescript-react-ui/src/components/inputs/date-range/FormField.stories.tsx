/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import * as zod from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form } from "../../forms/form";
import { Button } from "../../primitives/button";
import { DateRangePickerFormField } from "./FormField";

const meta = {
  title: "Inputs / Date Range Picker / Form Field",
  component: DateRangePickerFormField,
  decorators: [
    (Component) => (
      <div className="flex items-center justify-start px-4">
        <Component />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  description: string;
  label: string;
  min: Date;
  disabled: boolean;
}

/**
 * Form Schema
 */

const formSchema = zod.object({
  range: zod
    .object({
      from: zod.date(),
      to: zod.date(),
    })
    .required(),
});

type FormSchema = zod.infer<typeof formSchema>;

export const Default: StoryObj<Args> = {
  args: {
    description: "When do you want to go on vacation?",
    label: "Vacation Dates",
    min: new Date(),
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

    const values = form.watch();

    return (
      <div className="flex w-96 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <DateRangePickerFormField {...args} formSchema={formSchema} fieldName="range" />
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
