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
import { SelectIdFromListFormField } from "./SelectIdFromListFormField";

const meta = {
  title: "Forms / SelectIdFromListFormField",
  component: SelectIdFromListFormField,
} satisfies Meta;

export default meta;

/**
 * Mock Data
 */

interface Breed {
  id: string;
  name: string;
  origin: string;
}

const breeds: Breed[] = [
  { id: "YDoQ9gDFWV", name: "Labrador Retriever", origin: "Canada" },
  { id: "RnO9305Cx5", name: "German Shepherd", origin: "Germany" },
  { id: "prm9puC0E3", name: "Golden Retriever", origin: "Scotland" },
  { id: "Esuaoi1yZ1", name: "Bulldog", origin: "England" },
  { id: "Iuljc9HZDt", name: "Beagle", origin: "England" },
];

/**
 * Form Schema
 */

const formSchema = z.object({
  breedId: z.string().nullish(),
});

type FormValues = z.infer<typeof formSchema>;

export const SelectIdFromListFormFieldStory: StoryObj = {
  name: "SelectIdFromListFormField",
  render: () => {
    const form = useForm<FormValues>({
      resolver: zodResolver(formSchema),
    });

    const handleSubmit = form.handleSubmit(async (values) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <SelectIdFromListFormField
            fieldName="breedId"
            formSchema={formSchema}
            options={breeds}
            itemRenderer={(breed) => breed.name}
            placeholder="Select a breed"
            label="Breed"
            description="Select your favorite dog breed"
          />
          <Button className="mt-4" type="submit">
            Submit
          </Button>
        </Form>

        <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
