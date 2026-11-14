import { describe, expect, it } from "vitest";
import { capabilityTranslationKey } from "./capabilityTranslationKey";

describe("capabilityTranslationKey", () => {
  it("turns the namespace separator into a key segment", () => {
    expect(capabilityTranslationKey("patterns:finalize-order")).toBe("capabilities.patterns.finalize-order");
  });
});
