/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { RadioGroupFormField, RadioGroupFormFieldItem } from "./RadioGroupFormField";

/**
 * Args
 */

interface Args {
  disabled: boolean;
}

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / RadioGroupFormField",
};

export default meta;

/**
 * Form Schema
 */

const formSchema = z.object({
  size: z.enum(["sm", "md", "lg"]),
});

type FormSchema = z.infer<typeof formSchema>;

/**
 * Stories
 */

export const RadioGroupFormFieldStory: Story<Args> = (args) => {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
  });

  const handleSubmit = form.handleSubmit(async (values) => {
    await delay(300);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-80 flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <RadioGroupFormField label="Größe" fieldName="size" formSchema={formSchema} disabled={args.disabled}>
          <RadioGroupFormFieldItem value="sm">klein</RadioGroupFormFieldItem>
          <RadioGroupFormFieldItem value="md">mittel</RadioGroupFormFieldItem>
          <RadioGroupFormFieldItem value="lg">groß</RadioGroupFormFieldItem>
        </RadioGroupFormField>

        <Button type="submit" loading={form.formState.isSubmitting} className="mt-4">
          Submit
        </Button>
      </Form>

      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
RadioGroupFormFieldStory.storyName = "RadioGroupFormField";
RadioGroupFormFieldStory.args = {
  disabled: false,
};
