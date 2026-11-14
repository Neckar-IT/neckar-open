/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z as zod } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { exampleTags } from "../../inputs/tag-picker/exampleTags";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { TagPickerFormField } from "./TagPickerFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / TagPickerFormField",
};

export default meta;

/**
 * Form Schema
 */

const formSchema = zod.object({
  tags: zod
    .array(
      zod.object({
        id: zod.string(),
        name: zod.string(),
      }),
    )
    .min(1),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const TagPickerFormFieldStory: Story = () => {
  const form = useForm<FormSchema>({
    resolver: zodResolver(formSchema),
  });

  const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
    await delay(1000);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-lg flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <TagPickerFormField
          description="Please select your tags."
          formSchema={formSchema}
          label="Tags"
          fieldName="tags"
          options={exampleTags}
        />

        <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
          Submit
        </Button>
      </Form>

      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
TagPickerFormFieldStory.storyName = "TagPickerFormField";
