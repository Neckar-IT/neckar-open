/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type Story, type StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form } from "../../forms/form";
import { Button } from "../../primitives/button";
import { DatePickerFormField } from "./FormField";

/**
 * Meta Information
 */

export default {
  title: "Inputs / Date Picker / Form Field",
} satisfies StoryDefault;

/**
 * Form Schema
 */

const formSchema = z.object({
  dob: z.iso.date(),
});

type FormSchema = z.infer<typeof formSchema>;

interface Args {
  label: string;
  description: string;
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
        <DatePickerFormField {...args} formSchema={formSchema} fieldName="dob" max={new Date()} />
        <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
          Submit
        </Button>
      </Form>
      <CodeBlock className="mt-8">{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
Default.args = {
  description: "Your date of birth is used to calculate your age.",
  label: "Date of Birth",
  disabled: false,
};
