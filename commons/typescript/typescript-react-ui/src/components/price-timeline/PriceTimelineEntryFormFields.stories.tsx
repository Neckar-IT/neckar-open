import type { Meta, StoryObj } from "@storybook/react-vite";
import { useForm } from "react-hook-form";
import { MemoryRouter } from "react-router";
import { FormCard, FormCardContent } from "../forms/form";
import { PriceTimelineEntryFormFields } from "./PriceTimelineEntryFormFields";
import {
  priceTimelineEntryUpsertDefaultValues,
  usePriceTimelineEntryFormResolver,
  type PriceTimelineEntryUpsertRequestSchema,
} from "./usePriceTimelineEntryFormResolver";

const meta = {
  title: "Price timeline / PriceTimelineEntryFormFields",
} satisfies Meta;

export default meta;

export const PriceTimelineEntryFormFieldsStory: StoryObj<typeof meta> = {
  name: "PriceTimelineEntryFormFields",
  render: () => {
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
  },
};
