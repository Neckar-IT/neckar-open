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
import { NumberInputFormField } from "./NumberInputFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / NumberInputFormField",
};

export default meta;

/**
 * Args
 */

interface Args {
  description: string;
  label: string;
  min: number;
  max: number;
  step: number;
  fractionDigits: number;
  disabled: boolean;
}

/**
 * Form Schema
 */

const formSchema = zod.object({
  age: zod.number().min(18, "You must be at least 18 years old."),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const NumberInputFormFieldStory: Story<Args> = (args) => {
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
        <NumberInputFormField {...args} formSchema={formSchema} fieldName="age" />
        <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
          Submit
        </Button>
      </Form>

      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
NumberInputFormFieldStory.storyName = "NumberInputFormField";
NumberInputFormFieldStory.args = {
  description: "Your age in years.",
  label: "How old are you?",
  min: 0,
  max: 100,
  step: 1,
  fractionDigits: 0,
  disabled: false,
};
