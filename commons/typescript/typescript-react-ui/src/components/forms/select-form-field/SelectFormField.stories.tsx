/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import * as zod from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { SelectItem } from "../../primitives/select";
import { Form } from "../form";
import { SelectFormField } from "./SelectFormField";

/**
 * Meta Information
 */

const meta = {
  title: "Forms / SelectFormField",
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  label: string;
  description: string;
  disabled: boolean;
  isClearable: boolean;
}

/**
 * Form Schema
 */

const formSchema = zod.object({
  person: zod.string().nonempty(),
});

type FormSchema = zod.infer<typeof formSchema>;

/**
 * Stories
 */

export const SelectFormFieldStory: StoryObj<Args> = {
  name: "SelectFormField",
  args: {
    description: "Such dir einen aus!",
    label: "Welcher Michael ist der Beste?",
    disabled: false,
    isClearable: true,
  },
  render: ({ ...args }: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (values: FormSchema) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    return (
      <div className="w-80">
        <Form {...form} onSubmit={handleSubmit}>
          <SelectFormField {...args} formSchema={formSchema} fieldName="person">
            <SelectItem value="mike-tyson">Mike Tyson</SelectItem>
            <SelectItem value="michael-jordan">Michael Jordan</SelectItem>
            <SelectItem value="michael-schumacher">Michael Schumacher</SelectItem>
          </SelectFormField>
          <Button className="mt-4" type="submit" loading={form.formState.isSubmitting}>
            Abschicken
          </Button>
        </Form>

        <CodeBlock className="mt-8">{`value: ${JSON.stringify(form.watch(), null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
