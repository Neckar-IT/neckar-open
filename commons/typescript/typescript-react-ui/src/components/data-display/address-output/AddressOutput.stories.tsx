import type { AddressDraft } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../code-block";
import { AddressOutput } from "./AddressOutput";

const meta = {
  title: "Data display / AddressOutput",
  component: AddressOutput,
} satisfies Meta;

export default meta;

export const AddressOutputStory: StoryObj = {
  name: "AddressOutput",
  render: () => {
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
  },
};
