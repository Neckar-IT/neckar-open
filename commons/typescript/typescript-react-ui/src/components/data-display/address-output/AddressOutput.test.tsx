/**
 * Imports
 */

import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { AddressOutput } from "./AddressOutput";

/**
 * Tests
 *
 * AddressOutput renders street and city as a block, and the country only when asked for.
 */

const address = {
  street: "Neckarstraße",
  houseNumber: "12",
  zipCode: "72072",
  city: "Tübingen",
  country: "Germany",
};

describe("AddressOutput", () => {
  it("renders street and city without the country by default", () => {
    const { container } = render(<AddressOutput address={address} />);

    expect(container.textContent).toBe("Neckarstraße 1272072 Tübingen");
  });

  it("adds the country line with showCountry", () => {
    const { container } = render(<AddressOutput address={address} showCountry />);

    expect(container.textContent).toBe("Neckarstraße 1272072 TübingenGermany");
    expect(container.querySelectorAll("br")).toHaveLength(2);
  });
});
