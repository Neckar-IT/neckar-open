import type { Address } from "@neckar-hub/typescript-utils/openapi-types";
import { describe, expect, it } from "vitest";
import { addressDraftSchema, toAddressDraftFormValues } from "./AddressForms";

describe("toAddressDraftFormValues", () => {
  it("normalizes absent fields to null so the draft schema accepts them", () => {
    // Address fields are required on the wire since Address.Draft lost its Kotlin property
    // defaults. A backend running the previous model still omits fields holding their default —
    // this JSON shape (cast, no longer representable in the type) covers that rollout window.
    const legacyPartialAddress = { type: "Draft", street: "Musterweg" } as Address;

    const formValues = toAddressDraftFormValues(legacyPartialAddress);

    expect(formValues).toEqual({ street: "Musterweg", houseNumber: null, zipCode: null, city: null });
    expect(addressDraftSchema.safeParse(formValues).success).toBe(true);
  });

  it("keeps present values", () => {
    const address: Address = {
      type: "Draft",
      street: "Musterweg",
      houseNumber: "12",
      zipCode: "72070",
      city: "Tübingen",
      country: "Germany",
    };

    expect(toAddressDraftFormValues(address)).toEqual({ street: "Musterweg", houseNumber: "12", zipCode: "72070", city: "Tübingen" });
  });

  it("documents the trap: a partial legacy AddressDraft does NOT pass the schema without normalization", () => {
    const legacyPartialAddress = { type: "Draft", street: "Musterweg" } as Address;

    expect(addressDraftSchema.safeParse(legacyPartialAddress).success).toBe(false);
  });
});
