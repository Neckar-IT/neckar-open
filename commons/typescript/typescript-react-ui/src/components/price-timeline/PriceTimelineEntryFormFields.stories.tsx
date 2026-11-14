import type { Story, StoryDefault } from "@ladle/react";
import { useForm } from "react-hook-form";
import { MemoryRouter } from "react-router";
import { FormCard, FormCardContent } from "../forms/form";
import { PriceTimelineEntryFormFields } from "./PriceTimelineEntryFormFields";
import {
  priceTimelineEntryUpsertDefaultValues,
  usePriceTimelineEntryFormResolver,
  type PriceTimelineEntryUpsertRequestSchema,
} from "./usePriceTimelineEntryFormResolver";

export default {
  title: "Price timeline / PriceTimelineEntryFormFields",
} satisfies StoryDefault;

export const PriceTimelineEntryFormFieldsStory: Story = () => {
  const form = useForm<PriceTimelineEntryUpsertRequestSchema>({
    mode: "onTouched",
    resolver: usePriceTimelineEntryFormResolver(),
    defaultValues: priceTimelineEntryUpsertDefaultValues,
  });

  return (
    <MemoryRouter>
      <div className="max-w-md">
        <FormCard form={form}>
          <FormCardContent>
            <PriceTimelineEntryFormFields form={form} />
          </FormCardContent>
        </FormCard>
      </div>
    </MemoryRouter>
  );
};
PriceTimelineEntryFormFieldsStory.storyName = "PriceTimelineEntryFormFields";
