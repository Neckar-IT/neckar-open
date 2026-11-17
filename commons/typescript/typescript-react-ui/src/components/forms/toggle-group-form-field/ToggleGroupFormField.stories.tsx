/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { ToggleGroupFormField, ToggleGroupFormFieldItem } from "./ToggleGroupFormField";

/**
 * Args
 */

interface Args {
  disabled: boolean;
}

/**
 * Meta Information
 */

const meta = {
  title: "Forms / ToggleGroupFormField",
} satisfies Meta;

export default meta;

/**
 * Form Schema
 */

const formSchema = z.object({
  scope: z.enum(["perRoom", "acrossFloors"]),
});

type FormSchema = z.infer<typeof formSchema>;

/**
 * Stories
 */

export const ToggleGroupFormFieldStory: StoryObj<Args> = {
  name: "ToggleGroupFormField",
  args: {
    disabled: false,
  },
  render: (args: Args) => {
    const form = useForm<FormSchema>({
      resolver: zodResolver(formSchema),
      defaultValues: {
        scope: "perRoom",
      },
    });

    const handleSubmit = form.handleSubmit(async (values) => {
      await delay(300);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <ToggleGroupFormField label="Inklusivmenge gilt" fieldName="scope" formSchema={formSchema} disabled={args.disabled}>
            <ToggleGroupFormFieldItem value="perRoom">Pro Raum</ToggleGroupFormFieldItem>
            <ToggleGroupFormFieldItem value="acrossFloors">Gebäudeweit</ToggleGroupFormFieldItem>
          </ToggleGroupFormField>

          <Button type="submit" loading={form.formState.isSubmitting} className="mt-4">
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
