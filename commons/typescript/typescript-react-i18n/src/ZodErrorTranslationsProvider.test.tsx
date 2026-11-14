/**
 * Imports
 */

import { render } from "@testing-library/react";
import i18next from "i18next";
import { initReactI18next } from "react-i18next";
import { beforeEach, describe, expect, it } from "vitest";
import { z } from "zod";
import zodErrorsEn from "./locales/en/zod-errors.json";
import { ZodErrorTranslationsProvider } from "./ZodErrorTranslationsProvider";

/**
 * Tests
 *
 * These verify that the provider's i18n key paths actually resolve against the zod-errors.json
 * resource shape. A wrong path makes i18next fall back to rendering the raw key string.
 */

function firstMessage(result: z.ZodSafeParseResult<unknown>): string {
  const error = result.error;
  if (error === undefined) throw new Error("expected a parse failure");
  const issue = error.issues[0];
  if (issue === undefined) throw new Error("expected at least one issue");
  return issue.message;
}

describe("ZodErrorTranslationsProvider", () => {
  beforeEach(async () => {
    await i18next.use(initReactI18next).init({
      resources: {
        en: { zod: zodErrorsEn },
      },
      lng: "en",
      interpolation: { escapeValue: false },
    });
    render(<ZodErrorTranslationsProvider>{null}</ZodErrorTranslationsProvider>);
  });

  it("resolves the expected type name for invalid_type (not the raw types key)", () => {
    const message = firstMessage(z.number().safeParse("not a number"));
    expect(message).not.toContain("types.");
    expect(message.toLowerCase()).toContain("number");
  });

  it("resolves too_small for dates (not the raw key)", () => {
    const message = firstMessage(
      z
        .date()
        .min(new Date(2020, 0, 1))
        .safeParse(new Date(2019, 0, 1)),
    );
    expect(message).not.toContain("too_small");
  });

  it("resolves too_big for dates (not the raw key)", () => {
    const message = firstMessage(
      z
        .date()
        .max(new Date(2020, 0, 1))
        .safeParse(new Date(2021, 0, 1)),
    );
    expect(message).not.toContain("too_big");
  });

  it("resolves starts_with / ends_with format messages (not the raw key)", () => {
    const startsMessage = firstMessage(z.string().startsWith("foo").safeParse("bar"));
    expect(startsMessage).not.toContain("invalid_format");

    const endsMessage = firstMessage(z.string().endsWith("foo").safeParse("bar"));
    expect(endsMessage).not.toContain("invalid_format");
  });

  it("resolves the credit_card format name (not the raw validations key)", () => {
    const message = firstMessage(z.creditCard().safeParse("1234"));
    expect(message).not.toContain("validations.");
    expect(message.toLowerCase()).toContain("credit card");
  });

  it("resolves the iban format name (not the raw validations key)", () => {
    const message = firstMessage(z.iban().safeParse("DE89370400440532013001"));
    expect(message).not.toContain("validations.");
    expect(message).toContain("IBAN");
  });
});
