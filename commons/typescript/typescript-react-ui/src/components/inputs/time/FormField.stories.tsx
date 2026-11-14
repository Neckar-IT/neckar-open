/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type Story, type StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import * as zod from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form } from "../../forms/form";
import { Button } from "../../primitives/button";
import { TimeInputFormField } from "./FormField";

/**
 * Form Schema
 */

const formSchema = zod.object({
  time: zod.iso.time(),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / Time Input / Form Field",
};

export default meta;

/**
 * Args
 */

interface Args {
  description: string;
  label: string;
  disabled: boolean;
}

/**
 * Stories
 */

export const Default: Story<Args> = (args) => {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
  });

  const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
    await delay(1000);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="w-80">
      <Form {...form} onSubmit={handleSubmit}>
        <TimeInputFormField {...args} formSchema={formSchema} fieldName="time" />
        <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
          Submit
        </Button>
      </Form>

      <CodeBlock className="mt-8">{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
Default.args = {
  description: "The time is used to calculate the time of day.",
  label: "Time",
  disabled: false,
};
