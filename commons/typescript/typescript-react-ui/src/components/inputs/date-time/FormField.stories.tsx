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
import { DateTimePickerFormField } from "./FormField";

/**
 * Meta Information
 */

const meta = {
  title: "Inputs / Date Time Picker / Form Field",
  decorators: [
    (Component) => (
      <div className="flex items-center justify-start px-4">
        <Component />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

interface Args {
  label: string;
  description: string;
  disabled: boolean;
}

/**
 * Form Schema
 */

const formSchema = zod.object({
  begin: zod.date(),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const Default: StoryObj<Args> = {
  args: {
    description: "Geplanter Beginn der Produktion.",
    disabled: false,
    label: "Produktionsbeginn",
  },
  render: (args: Args) => {
    const today = new Date(2024, 5, 28, 14, 31, 0);

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
          <DateTimePickerFormField {...args} formSchema={formSchema} min={today} fieldName="begin" />
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
