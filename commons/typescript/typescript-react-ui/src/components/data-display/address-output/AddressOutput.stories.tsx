import type { Story, StoryDefault } from "@ladle/react";
import type { AddressDraft } from "@neckar-hub/typescript-utils/openapi-types";
import { CodeBlock } from "../code-block";
import { AddressOutput } from "./AddressOutput";

const meta: StoryDefault = {
  title: "Data display / AddressOutput",
};

export default meta;

export const AddressOutputStory: Story = () => {
  const address: AddressDraft = {
    street: "Ulrichstr.",
    houseNumber: "25",
    zipCode: "72116",
    city: "Mössingen",
    country: "Germany",
  };

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-1">
        <CodeBlock>variant: "block" (default)</CodeBlock>
        <AddressOutput address={address} />
      </div>
      <div className="flex flex-col gap-1">
        <CodeBlock>variant: "inline"</CodeBlock>
        <AddressOutput address={address} variant="inline" />
      </div>
    </div>
  );
};
AddressOutputStory.storyName = "AddressOutput";
