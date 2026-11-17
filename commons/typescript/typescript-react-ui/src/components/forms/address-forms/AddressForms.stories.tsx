import type { Address, AddressConfirmedWithType, AddressDraftWithType } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { AddressConfirmedForm, AddressDraftForm } from "./AddressForms";

/**
 * Meta Information
 */
const meta = {
  title: "Forms / Address Forms",
} satisfies Meta;

export default meta;

/**
 * Mock Data
 */
const address: Address = {
  type: "Draft",
  street: null,
  houseNumber: null,
  zipCode: null,
  city: null,
  country: null,
};

/**
 * Address Draft Form Story
 */
export const AddressDraftFormStory: StoryObj<typeof meta> = {
  name: "Address Draft Form",
  render: () => {
    const handleSubmit = async (address: AddressDraftWithType) => {
      await delay(500);
      window.alert(`Submitted address: ${JSON.stringify(address, null, 2)}`);
    };

    return (
      <AddressDraftForm className="mx-auto w-2xl" address={address} onCancel={() => window.alert("cancelled")} onSubmit={handleSubmit} />
    );
  },
};

/**
 * Address Confirmed Form Story
 */
export const AddressConfirmedFormStory: StoryObj<typeof meta> = {
  name: "Address Confirmed Form",
  render: () => {
    const handleSubmit = async (address: AddressConfirmedWithType) => {
      await delay(500);
      window.alert(`Submitted address: ${JSON.stringify(address, null, 2)}`);
    };

    return (
      <AddressConfirmedForm
        className="mx-auto w-2xl"
        address={address}
        onCancel={() => window.alert("cancelled")}
        onSubmit={handleSubmit}
      />
    );
  },
};
