/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import { createZodEnumFromConstObject } from "@neckar-hub/typescript-utils/zod-utils";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { useForm } from "react-hook-form";
import z from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Button } from "../../primitives/button";
import { Form } from "../form";
import { SelectFromEnumFormField } from "./SelectFromEnumFormField";

/**
 * Meta Information
 */

const meta = {
  title: "Forms / SelectFromEnumFormField",
} satisfies Meta;

export default meta;

/**
 * Example Enum
 */

const ChickenBreed = {
  Silkie: "Silkie",
  RhodeIslandRed: "RhodeIslandRed",
  AyamCemani: "AyamCemani",
  Leghorn: "Leghorn",
  Orpington: "Orpington",
} as const;

/**
 * Form Schema
 */

const formSchema = z.object({
  breed: createZodEnumFromConstObject(ChickenBreed).nullish(),
});

type FormValues = z.infer<typeof formSchema>;

/**
 * Stories
 */

export const SelectFroEnumFormFieldStory: StoryObj<typeof meta> = {
  name: "SelectFromEnumFormField",
  render: () => {
    const form = useForm<FormValues>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (values) => {
      await delay(500);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <SelectFromEnumFormField formSchema={formSchema} fieldName="breed" label="Breed" placeholder="Please select a breed" />
          <Button type="submit" className="mt-4" loading={form.formState.isSubmitting}>
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
