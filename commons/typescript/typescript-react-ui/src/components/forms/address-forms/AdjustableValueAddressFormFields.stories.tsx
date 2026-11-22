/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { AdjustableValueAddressDraft } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form } from "../form";
import { addressConfirmedSchema } from "./AddressForms";
import { AdjustableValueAddressFormFields, adjustableValueAddressSchema } from "./AdjustableValueAddressFormFields";

const meta = {
  title: "Forms / Address Forms",
  component: AdjustableValueAddressFormFields,
} satisfies Meta;

export default meta;

/**
 * Mock Data
 */

const calculatedAddress = { street: "Rechenweg", houseNumber: "1", zipCode: "72070", city: "Tübingen", country: "Germany" };
const typedAddress = { street: "Handstraße", houseNumber: "2", zipCode: "72108", city: "Rottenburg", country: "Germany" };

const formSchema = z.object({
  jobSiteLocation: adjustableValueAddressSchema(addressConfirmedSchema.extend({ country: z.string() })),
});

/**
 * Renders the fields next to the form values, so the effect of the pencil and the undo action stays visible
 */

function AdjustableValueAddressForm({ jobSiteLocation }: { jobSiteLocation: AdjustableValueAddressDraft }): React.ReactElement {
  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: { jobSiteLocation } as z.infer<typeof formSchema>,
  });

  return (
    <Form {...form} className="mx-auto flex w-2xl flex-col gap-8">
      <AdjustableValueAddressFormFields formSchema={formSchema} schemaKey="jobSiteLocation" />
      <FormValues />
    </Form>
  );
}

function FormValues(): React.ReactElement {
  const values = useWatch({ name: "jobSiteLocation" }) as AdjustableValueAddressDraft;

  return <CodeBlock>{`jobSiteLocation: ${JSON.stringify(values, null, 2)}`}</CodeBlock>;
}

export const AdjustableValueAddressAutoStory: StoryObj = {
  name: "AdjustableValueAddressFormFields (Auto)",
  render: () => <AdjustableValueAddressForm jobSiteLocation={{ type: "Auto", value: calculatedAddress }} />,
};

export const AdjustableValueAddressAdjustedStory: StoryObj = {
  name: "AdjustableValueAddressFormFields (Adjusted)",
  render: () => <AdjustableValueAddressForm jobSiteLocation={{ type: "Adjusted", calculated: calculatedAddress, value: typedAddress }} />,
};

export const AdjustableValueAddressManualStory: StoryObj = {
  name: "AdjustableValueAddressFormFields (Manual)",
  render: () => <AdjustableValueAddressForm jobSiteLocation={{ type: "Manual", value: typedAddress }} />,
};
