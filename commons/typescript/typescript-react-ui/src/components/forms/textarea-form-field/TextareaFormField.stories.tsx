/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { type Story, type StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import * as zod from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { TextareaFormField } from "./TextareaFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / TextareaFormField",
};

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

/**
 * Stories
 */

export const TextareaFormFieldStory: Story<Args> = (args) => {
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
};
TextareaFormFieldStory.storyName = "TextareaFormField";
TextareaFormFieldStory.args = {
  description: "Please be honest.",
  label: "What is your Opinion?",
  maxLength: 256,
  rows: 3,
  disabled: false,
};
TextareaFormFieldStory.argTypes = {
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
};
