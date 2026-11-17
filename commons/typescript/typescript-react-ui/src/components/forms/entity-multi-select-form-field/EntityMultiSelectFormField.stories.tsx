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
import { Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../primitives/dialog";
import { Form } from "../form";
import { EntityMultiSelectFormField } from "./EntityMultiSelectFormField";

/**
 * Meta Information
 */

const meta = {
  title: "Forms / EntityMultiSelectFormField",
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
  breedIds: z.array(z.string()),
});

type FormValues = z.infer<typeof formSchema>;

/**
 * Select Dialog (caller-provided)
 */

interface BreedSelectDialogProps {
  onOpenChange: (open: boolean) => void;
  selectedBreedIds: string[];
  onSelectBreed: (breed: Breed | undefined) => void;
}

function BreedSelectDialog({ onOpenChange, selectedBreedIds, onSelectBreed }: BreedSelectDialogProps) {
  return (
    <Dialog open={true} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>Select a breed</DialogTitle>
        </DialogHeader>
        <DialogBody>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left">
                <th className="py-2">Name</th>
                <th className="py-2">Origin</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {breeds.map((breed) => (
                <tr key={breed.id}>
                  <td className="py-2">{breed.name}</td>
                  <td className="py-2">{breed.origin}</td>
                  <td className="py-2 text-right">
                    <Button variant="outline" size="sm" disabled={selectedBreedIds.includes(breed.id)} onClick={() => onSelectBreed(breed)}>
                      Select
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </DialogBody>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Stories
 */

export const EntityMultiSelectFormFieldStory: StoryObj<typeof meta> = {
  name: "EntityMultiSelectFormField",
  render: () => {
    const form = useForm<FormValues>({
      resolver: zodResolver(formSchema),
      defaultValues: { breedIds: [] },
    });

    const handleSubmit = form.handleSubmit(async (values) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <EntityMultiSelectFormField<typeof formSchema, Breed>
            fieldName="breedIds"
            formSchema={formSchema}
            initialEntities={[]}
            addButtonLabel="Add breed"
            label="Breeds"
            description="Click the add button to open the selection dialog"
            itemRenderer={(breed) => breed.name}
            renderDialog={({ onOpenChange, selectedEntityIds, onSelectEntity }) => (
              <BreedSelectDialog onOpenChange={onOpenChange} selectedBreedIds={selectedEntityIds} onSelectBreed={onSelectEntity} />
            )}
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

export const EntityMultiSelectFormFieldStoryWithInitialSelection: StoryObj<typeof meta> = {
  name: "EntityMultiSelectFormField (with initial selection)",
  render: () => {
    const initialBreeds = [breeds[0]!, breeds[4]!];

    const form = useForm<FormValues>({
      resolver: zodResolver(formSchema),
      defaultValues: { breedIds: initialBreeds.map((breed) => breed.id) },
    });

    const handleSubmit = form.handleSubmit(async (values) => {
      await delay(1000);
      window.alert(JSON.stringify(values, null, 2));
    });

    const values = form.watch();

    return (
      <div className="flex w-80 flex-col gap-8">
        <Form {...form} onSubmit={handleSubmit}>
          <EntityMultiSelectFormField<typeof formSchema, Breed>
            fieldName="breedIds"
            formSchema={formSchema}
            initialEntities={initialBreeds}
            addButtonLabel="Add breed"
            label="Breeds"
            description="Pre-selected; already-picked breeds are disabled in the dialog"
            itemRenderer={(breed) => `${breed.name} (${breed.origin})`}
            renderDialog={({ onOpenChange, selectedEntityIds, onSelectEntity }) => (
              <BreedSelectDialog onOpenChange={onOpenChange} selectedBreedIds={selectedEntityIds} onSelectBreed={onSelectEntity} />
            )}
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
