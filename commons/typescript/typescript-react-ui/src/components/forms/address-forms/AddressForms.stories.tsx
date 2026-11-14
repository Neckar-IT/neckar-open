import type { Story, StoryDefault } from "@ladle/react";
import type { Address, AddressConfirmedWithType, AddressDraftWithType } from "@neckar-hub/typescript-utils/openapi-types";
import { delay } from "es-toolkit";
import { AddressConfirmedForm, AddressDraftForm } from "./AddressForms";

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
export const AddressDraftFormStory: Story = () => {
  const handleSubmit = async (address: AddressDraftWithType) => {
    await delay(500);
    window.alert(`Submitted address: ${JSON.stringify(address, null, 2)}`);
  };

  return (
    <AddressDraftForm className="mx-auto w-2xl" address={address} onCancel={() => window.alert("cancelled")} onSubmit={handleSubmit} />
  );
};
AddressDraftFormStory.storyName = "Address Draft Form";

/**
 * Address Confirmed Form Story
 */
export const AddressConfirmedFormStory: Story = () => {
  const handleSubmit = async (address: AddressConfirmedWithType) => {
    await delay(500);
    window.alert(`Submitted address: ${JSON.stringify(address, null, 2)}`);
  };

  return (
    <AddressConfirmedForm className="mx-auto w-2xl" address={address} onCancel={() => window.alert("cancelled")} onSubmit={handleSubmit} />
  );
};
AddressConfirmedFormStory.storyName = "Address Confirmed Form";
