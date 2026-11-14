import { describe, expect, it } from "vitest";
import { transformLarge } from "./transformLarge";

describe("transformLarge", () => {
  it("should wrap the inflated string with start and end markers", () => {
    const result = transformLarge("Speichern");

    expect(result.startsWith("«")).toBe(true);
    expect(result.endsWith("»")).toBe(true);
  });

  it("should fill with the ÅŴÑgjp pattern only", () => {
    const result = transformLarge("Speichern");
    const content = result.slice(1, -1);

    expect(content).toMatch(/^[ÅŴÑgjp]+$/);
  });

  it("should inflate short texts more dramatically than long texts", () => {
    const shortContent = transformLarge("OK").slice(1, -1);
    const mediumContent = transformLarge("Speichern").slice(1, -1);
    const longContent = transformLarge("Der Autor kann aufgrund von Validierungsfehlern nicht bestätigt werden.").slice(1, -1);

    const shortFactor = shortContent.length / 2;
    const mediumFactor = mediumContent.length / 9;
    const longFactor = longContent.length / 71;

    expect(shortFactor).toBeGreaterThan(mediumFactor);
    expect(mediumFactor).toBeGreaterThan(longFactor);
  });

  it("should match the MIN_ADDITION + ceil(len * 1.5) formula", () => {
    expect(transformLarge("OK").slice(1, -1).length).toBe(15);
    expect(transformLarge("Speichern").slice(1, -1).length).toBe(26);
    expect(transformLarge("Autor erfolgreich gespeichert.").slice(1, -1).length).toBe(57);
  });

  it("should inflate the empty string to the minimum addition length", () => {
    const content = transformLarge("").slice(1, -1);

    expect(content.length).toBe(12);
    expect(content).toMatch(/^[ÅŴÑgjp]+$/);
  });

  it("should treat the input as a single segment regardless of placeholders", () => {
    const content = transformLarge("Hallo {{name}}").slice(1, -1);

    expect(content).toMatch(/^[ÅŴÑgjp]+$/);
    expect(content).not.toContain("{{");
  });
});
