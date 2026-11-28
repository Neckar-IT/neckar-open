import type { Resource } from "i18next";
import { describe, expect, it } from "vitest";
import { createGermanT } from "./createGermanT";

const resources: Resource = {
  de: {
    translation: {
      greeting: "Guten Tag",
      orderOf: "Bestellung von {{name}}",
    },
  },
};

describe("createGermanT", () => {
  it("returns the German text of a key", async () => {
    const t = await createGermanT(resources);

    expect(t("greeting")).toBe("Guten Tag");
  });

  it("interpolates a value without escaping HTML", async () => {
    const t = await createGermanT(resources);

    expect(t("orderOf", { name: "<Müller & Söhne>" })).toBe("Bestellung von <Müller & Söhne>");
  });
});
