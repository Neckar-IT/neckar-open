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
import { SwitchFormField } from "./SwitchFormField";

const meta = {
  title: "Forms / SwitchFormField",
  component: SwitchFormField,
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  label: string;
  description: string;
  disabled: boolean;
}

/**
 * Form Schema
 */

const formSchema = zod.object({
  allow: zod.boolean(),
});

type FormSchema = zod.infer<typeof formSchema>;

export const SwitchFormFieldStory: StoryObj<Args> = {
  name: "SwitchFormField",
  args: {
    label: "Allow comments",
    description: "Should you be allowed to post comments?",
    disabled: false,
  },
  render: (args: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      if (!values.allow) {
        form.setError("allow", { message: "You must allow comments" });
        return;
      }

      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-96 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <SwitchFormField {...args} formSchema={formSchema} fieldName="allow" />
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
