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
import { PasswordInputFormField } from "./PasswordInputFormField";

const meta = {
  title: "Forms / PasswordInputFormField",
  component: PasswordInputFormField,
} satisfies Meta;

export default meta;

/**
 * Form Schema
 */

const formSchema = zod.object({
  password: zod.string().trim().min(1, "Please provide your password."),
});

type FormSchema = zod.infer<typeof formSchema>;

export const PasswordInputFormFieldStory: StoryObj = {
  name: "PasswordInputFormField",
  render: () => {
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
          <PasswordInputFormField formSchema={formSchema} fieldName="password" />
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
