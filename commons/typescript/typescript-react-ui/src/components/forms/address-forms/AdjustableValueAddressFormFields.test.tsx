/**
 * Imports
 */

import { zodResolver } from "@hookform/resolvers/zod";
import {
  type AdjustableValueAddressDraft,
  AdjustableValueAdjustedAddressDraftWithTypeType,
  AdjustableValueAutoAddressDraftWithTypeType,
  AdjustableValueManualAddressDraftWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { Form } from "../form";
import { addressConfirmedSchema } from "./AddressForms";
import {
  AdjustableValueAddressFormFields,
  adjustableValueAddressSchema,
  overrideCalculatedAddress,
  resetToCalculatedAddress,
} from "./AdjustableValueAddressFormFields";

/**
 * Tests
 *
 * An adjustable address is `Auto` (the calculated address, fields disabled), `Adjusted` (a user override of a calculated
 * address, fields editable and resettable) or `Manual` (a user address without any calculated counterpart, fields
 * editable with nothing to reset to).
 */

const { Adjusted } = AdjustableValueAdjustedAddressDraftWithTypeType;
const { Auto } = AdjustableValueAutoAddressDraftWithTypeType;
const { Manual } = AdjustableValueManualAddressDraftWithTypeType;

const calculatedAddress = { street: "Rechenweg", houseNumber: "1", zipCode: "72070", city: "Tübingen", country: "Germany" };
const typedAddress = { street: "Handstraße", houseNumber: "2", zipCode: "72108", city: "Rottenburg", country: "Germany" };

const formSchema = z.object({
  jobSiteLocation: adjustableValueAddressSchema(addressConfirmedSchema.extend({ country: z.string() })),
});

/**
 * The address inputs are found by their field path: the label of a form field is not associated with its input
 * (`TextInput` receives no `id`), so `getByLabelText` cannot reach them.
 */

function addressInput(field: "street" | "city"): HTMLInputElement {
  const input = document.querySelector<HTMLInputElement>(`input[name="jobSiteLocation.value.${field}"]`);

  if (input === null) {
    throw new Error(`No input rendered for the address field "${field}"`);
  }

  return input;
}

function TestForm({ jobSiteLocation }: { jobSiteLocation: AdjustableValueAddressDraft }): React.ReactElement {
  const form = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: { jobSiteLocation } as z.infer<typeof formSchema>,
  });

  return (
    <Form {...form} onSubmit={form.handleSubmit(() => {})}>
      <AdjustableValueAddressFormFields formSchema={formSchema} schemaKey="jobSiteLocation" />
    </Form>
  );
}

describe("AdjustableValueAddressFormFields", () => {
  describe("Auto mode", () => {
    const auto = { type: Auto, value: calculatedAddress } as const;

    it("shows the calculated address in disabled fields", () => {
      render(<TestForm jobSiteLocation={auto} />);

      expect(addressInput("street")).toHaveValue("Rechenweg");
      expect(addressInput("street")).toBeDisabled();
      expect(addressInput("city")).toBeDisabled();
    });

    it("makes the address editable when the override action is used, without changing what is displayed", async () => {
      const user = userEvent.setup();
      render(<TestForm jobSiteLocation={auto} />);

      await user.click(screen.getByRole("button", { name: "Override the calculated address" }));

      expect(addressInput("street")).toHaveValue("Rechenweg");
      expect(addressInput("street")).toBeEnabled();
      expect(screen.getByRole("button", { name: "Return to the calculated address" })).toBeInTheDocument();
    });

    it("offers no reset, as the calculated address is already the effective one", () => {
      render(<TestForm jobSiteLocation={auto} />);

      expect(screen.queryByRole("button", { name: "Return to the calculated address" })).toBeNull();
      expect(screen.queryByRole("button", { name: "Show the calculated address" })).toBeNull();
    });
  });

  describe("Adjusted mode", () => {
    const adjusted = { type: Adjusted, calculated: calculatedAddress, value: typedAddress } as const;

    it("shows the override in editable fields", () => {
      render(<TestForm jobSiteLocation={adjusted} />);

      expect(addressInput("street")).toHaveValue("Handstraße");
      expect(addressInput("street")).toBeEnabled();
    });

    it("reveals the calculated address in the info popover", async () => {
      const user = userEvent.setup();
      render(<TestForm jobSiteLocation={adjusted} />);

      await user.click(screen.getByRole("button", { name: "Show the calculated address" }));

      // The popover renders the calculated address as two lines inside one element, so the whole content is asserted
      const popover = screen.getByRole("dialog");
      expect(popover).toHaveTextContent("Calculated Value:");
      expect(popover).toHaveTextContent("Rechenweg 1");
      expect(popover).toHaveTextContent("72070 Tübingen");
    });

    it("restores the calculated address in disabled fields when the reset action is used", async () => {
      const user = userEvent.setup();
      render(<TestForm jobSiteLocation={adjusted} />);

      await user.click(screen.getByRole("button", { name: "Return to the calculated address" }));

      expect(addressInput("street")).toHaveValue("Rechenweg");
      expect(addressInput("city")).toHaveValue("Tübingen");
      expect(addressInput("street")).toBeDisabled();
    });
  });

  describe("Manual mode", () => {
    const manual = { type: Manual, value: typedAddress } as const;

    it("shows the typed address in editable fields", () => {
      render(<TestForm jobSiteLocation={manual} />);

      expect(addressInput("street")).toHaveValue("Handstraße");
      expect(addressInput("street")).toBeEnabled();
    });

    it("offers neither a reset nor an info popover, as no address was ever calculated", () => {
      render(<TestForm jobSiteLocation={manual} />);

      expect(screen.queryByRole("button", { name: "Return to the calculated address" })).toBeNull();
      expect(screen.queryByRole("button", { name: "Override the calculated address" })).toBeNull();
      expect(screen.queryByRole("button", { name: "Show the calculated address" })).toBeNull();
    });
  });
});

describe("overrideCalculatedAddress", () => {
  it("turns the calculated address of an Auto value into an override that keeps it as fallback", () => {
    expect(overrideCalculatedAddress({ type: Auto, value: calculatedAddress })).toEqual({
      type: Adjusted,
      calculated: calculatedAddress,
      value: calculatedAddress,
    });
  });

  it("keeps an Adjusted value, which already overrides a calculated address", () => {
    const adjusted = { type: Adjusted, calculated: calculatedAddress, value: typedAddress } as const;

    expect(overrideCalculatedAddress(adjusted)).toEqual(adjusted);
  });

  it("keeps a Manual value, which has no calculated address to override", () => {
    const manual = { type: Manual, value: typedAddress } as const;

    expect(overrideCalculatedAddress(manual)).toEqual(manual);
  });
});

describe("resetToCalculatedAddress", () => {
  it("discards the override of an Adjusted value and returns to Auto", () => {
    expect(resetToCalculatedAddress({ type: Adjusted, calculated: calculatedAddress, value: typedAddress })).toEqual({
      type: Auto,
      value: calculatedAddress,
    });
  });

  it("keeps an Auto value, which carries no override", () => {
    const auto = { type: Auto, value: calculatedAddress } as const;

    expect(resetToCalculatedAddress(auto)).toEqual(auto);
  });

  it("keeps a Manual value: it has nothing to reset to, so this does not end up on Auto", () => {
    const manual = { type: Manual, value: typedAddress } as const;

    expect(resetToCalculatedAddress(manual)).toEqual(manual);
  });
});
