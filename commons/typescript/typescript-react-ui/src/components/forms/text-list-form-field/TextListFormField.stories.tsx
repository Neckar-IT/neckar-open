/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z as zod } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { TextListFormField } from "./TextListFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / TextListFormField",
};

export default meta;

/**
 * Form Schema
 */

const formSchema = zod.object({
  notes: zod.array(zod.string().trim().min(1)).max(3),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const TextListFormFieldStory: Story = () => {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
    defaultValues: { notes: ["Zähler steht im Keller"] },
  });

  const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
    await delay(1000);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-lg flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <TextListFormField description="At most three lines." formSchema={formSchema} label="Notes" fieldName="notes" maxEntries={3} />

        <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
          Submit
        </Button>
      </Form>

      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
TextListFormFieldStory.storyName = "TextListFormField";
