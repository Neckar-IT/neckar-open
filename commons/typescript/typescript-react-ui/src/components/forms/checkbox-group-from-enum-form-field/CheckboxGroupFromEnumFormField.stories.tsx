/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import { createZodEnumFromConstObject } from "@neckar-hub/typescript-utils/zod-utils";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { CheckboxGroupFromEnumFormField } from "./CheckboxGroupFromEnumFormField";

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
  title: "Forms / CheckboxGroupFromEnumFormField",
};

export default meta;

/**
 * Example Enum
 */

const ContentWarning = {
  Violence: "Violence",
  Death: "Death",
  SelfHarm: "SelfHarm",
  StrongLanguage: "StrongLanguage",
} as const;

/**
 * Form Schema
 */

const formSchema = z.object({
  contentWarnings: z.array(createZodEnumFromConstObject(ContentWarning)).min(1),
});

type FormValues = z.infer<typeof formSchema>;

/**
 * Stories
 */

export const CheckboxGroupFromEnumFormFieldStory: Story<Args> = (args) => {
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      contentWarnings: [ContentWarning.Death],
    },
  });

  const handleSubmit = form.handleSubmit(async (values) => {
    await delay(500);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-96 flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <CheckboxGroupFromEnumFormField
          formSchema={formSchema}
          fieldName="contentWarnings"
          enumName="ContentWarning"
          label="Inhaltswarnungen"
          disabled={args.disabled}
        />
        <Button type="submit" className="mt-4" loading={form.formState.isSubmitting}>
          Submit
        </Button>
      </Form>

      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
CheckboxGroupFromEnumFormFieldStory.storyName = "CheckboxGroupFromEnumFormField";
CheckboxGroupFromEnumFormFieldStory.args = {
  disabled: false,
};
