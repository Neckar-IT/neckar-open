/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import type { Story, StoryDefault } from "@ladle/react";
import type { AdjustableValueAddressDraft } from "@neckar-hub/typescript-utils/openapi-types";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { CodeBlock } from "../../data-display/code-block";
import { Form } from "../form";
import { addressConfirmedSchema } from "./AddressForms";
import { AdjustableValueAddressFormFields, adjustableValueAddressSchema } from "./AdjustableValueAddressFormFields";

/**
 * Meta Information
 */
const meta: StoryDefault = {
  title: "Forms / Address Forms",
};

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

/**
 * Stories
 */

export const AdjustableValueAddressAutoStory: Story = () => (
  <AdjustableValueAddressForm jobSiteLocation={{ type: "Auto", value: calculatedAddress }} />
);
AdjustableValueAddressAutoStory.storyName = "AdjustableValueAddressFormFields (Auto)";

export const AdjustableValueAddressAdjustedStory: Story = () => (
  <AdjustableValueAddressForm jobSiteLocation={{ type: "Adjusted", calculated: calculatedAddress, value: typedAddress }} />
);
AdjustableValueAddressAdjustedStory.storyName = "AdjustableValueAddressFormFields (Adjusted)";

export const AdjustableValueAddressManualStory: Story = () => (
  <AdjustableValueAddressForm jobSiteLocation={{ type: "Manual", value: typedAddress }} />
);
AdjustableValueAddressManualStory.storyName = "AdjustableValueAddressFormFields (Manual)";
