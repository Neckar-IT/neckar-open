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
import { Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../primitives/dialog";
import { Form } from "../form";
import { EntitySelectFormField } from "./EntitySelectFormField";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Forms / EntitySelectFormField",
};

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

/**
 * Select Dialog (caller-provided)
 */

interface BreedSelectDialogProps {
  onOpenChange: (open: boolean) => void;
  selectedBreed: Breed | undefined;
  onSelectBreed: (breed: Breed | undefined) => void;
}

function BreedSelectDialog({ onOpenChange, selectedBreed, onSelectBreed }: BreedSelectDialogProps) {
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
              {breeds.map((breed) => {
                const isSelected = breed.id === selectedBreed?.id;
                return (
                  <tr key={breed.id} className={isSelected ? "bg-muted" : undefined}>
                    <td className="py-2">{breed.name}</td>
                    <td className="py-2">{breed.origin}</td>
                    <td className="py-2 text-right">
                      <Button variant="outline" size="sm" onClick={() => onSelectBreed(breed)}>
                        Select
                      </Button>
                    </td>
                  </tr>
                );
              })}
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

export const EntitySelectFormFieldStory: Story = () => {
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
        <EntitySelectFormField<typeof formSchema, Breed>
          fieldName="breedId"
          formSchema={formSchema}
          initialEntity={undefined}
          placeholder="Select a breed"
          label="Breed"
          description="Click the field to open the selection dialog"
          itemRenderer={(breed) => breed.name}
          renderDialog={({ onOpenChange, selectedEntity, onSelectEntity }) => (
            <BreedSelectDialog onOpenChange={onOpenChange} selectedBreed={selectedEntity} onSelectBreed={onSelectEntity} />
          )}
        />
        <Button className="mt-4" type="submit">
          Submit
        </Button>
      </Form>

      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
EntitySelectFormFieldStory.storyName = "EntitySelectFormField";

export const EntitySelectFormFieldStoryWithInitialSelection: Story = () => {
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: { breedId: breeds[0]!.id },
  });

  const handleSubmit = form.handleSubmit(async (values) => {
    await delay(1000);
    window.alert(JSON.stringify(values, null, 2));
  });

  const values = form.watch();

  return (
    <div className="flex w-80 flex-col gap-8">
      <Form {...form} onSubmit={handleSubmit}>
        <EntitySelectFormField<typeof formSchema, Breed>
          fieldName="breedId"
          formSchema={formSchema}
          initialEntity={breeds[0]!}
          placeholder="Select a breed"
          label="Breed"
          description="Pre-selected; use the clear button to reset"
          itemRenderer={(breed) => `${breed.name} (${breed.origin})`}
          renderDialog={({ onOpenChange, selectedEntity, onSelectEntity }) => (
            <BreedSelectDialog onOpenChange={onOpenChange} selectedBreed={selectedEntity} onSelectBreed={onSelectEntity} />
          )}
        />
        <Button className="mt-4" type="submit">
          Submit
        </Button>
      </Form>

      <CodeBlock>{`values: ${JSON.stringify(values, null, 2)}`}</CodeBlock>
    </div>
  );
};
EntitySelectFormFieldStoryWithInitialSelection.storyName = "EntitySelectFormField (with initial selection)";
