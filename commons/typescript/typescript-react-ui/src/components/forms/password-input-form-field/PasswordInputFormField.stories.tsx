/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type Story, type StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z as zod } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { PasswordInputFormField } from "./PasswordInputFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / PasswordInputFormField",
};

export default meta;

/**
 * Form Schema
 */

const formSchema = zod.object({
  password: zod.string().trim().min(1, "Please provide your password."),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const PasswordInputFormFieldStory: Story = () => {
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
};
PasswordInputFormFieldStory.storyName = "PasswordInputFormField";
PasswordInputFormFieldStory.args = {
  label: "Password",
  disabled: false,
  required: true,
};
