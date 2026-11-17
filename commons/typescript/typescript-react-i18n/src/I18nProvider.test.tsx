/**
 * Imports
 */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useTranslation } from "react-i18next";
import { describe, expect, it } from "vitest";
import { z as zod } from "zod";
import { I18nProvider } from "./I18nProvider";
import { useSetLanguage } from "./utility-hooks/useSetLanguage";

/**
 * Content with zod errors
 */

function Content(): React.ReactElement {
  const setLanguage = useSetLanguage();
  const { t } = useTranslation();

  const schema = zod.object({
    undefined: zod.string(),

    email: zod.email(),
    url: zod.url(),
    uuid: zod.uuid(),
    guid: zod.guid(),
    emptyString: zod.string().nonempty(),
    stringTooShort: zod.string().min(5),
    stringLength: zod.string().length(5),
    stringTooLong: zod.string().max(5),

    numberLessThan: zod.number().lt(5),
    numberLessThanOrEqual: zod.number().lte(5),
    numberGreaterThan: zod.number().gt(5),
    numberGreaterThanOrEqual: zod.number().gte(5),
    numberMultipleOf: zod.number().multipleOf(5),
  });

  const results = schema.safeParse({
    email: "not an email",
    url: "not an url",
    uuid: "not a uuid",
    guid: "not a guid",
    emptyString: "",
    stringTooShort: "min",
    stringLength: "min",
    stringTooLong: "really long",

    numberLessThan: 8,
    numberLessThanOrEqual: 8,
    numberGreaterThan: 3,
    numberGreaterThanOrEqual: 3,
    numberMultipleOf: 8,
  });

  return (
    <div>
      {results.error?.issues.map((issue) => (
        <div key={issue.path.join(".")} data-testid={issue.path.join(".")}>
          {issue.message}
        </div>
      ))}
      <button data-testid="set-language" onClick={() => setLanguage("de")}>
        {t("set-language")}
      </button>
    </div>
  );
}

/**
 * Tests
 */

describe("I18nProvider", () => {
  it("should use the language resources passed to the I18nProvider", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider
        languageResources={{
          de: {
            translation: {
              "set-language": "Sprache ändern",
            },
          },
          en: {
            translation: {
              "set-language": "Change language",
            },
          },
        }}
      >
        <Content />
      </I18nProvider>,
    );

    expect(screen.getByTestId("set-language").innerHTML).toBe("Change language");

    await user.click(screen.getByTestId("set-language"));

    expect(screen.getByTestId("set-language").innerHTML).toBe("Sprache ändern");
  });

  it("should render zod errors in english by default", () => {
    render(
      <I18nProvider>
        <Content />
      </I18nProvider>,
    );

    expect(screen.getByTestId("undefined").innerHTML).toBe("Required");

    expect(screen.getByTestId("email").innerHTML).toBe("Invalid email");
    expect(screen.getByTestId("url").innerHTML).toBe("Invalid url");
    expect(screen.getByTestId("uuid").innerHTML).toBe("Invalid identifier");
    expect(screen.getByTestId("guid").innerHTML).toBe("Invalid identifier");
    expect(screen.getByTestId("emptyString").innerHTML).toBe("Required");
    expect(screen.getByTestId("stringTooShort").innerHTML).toBe("String must contain at least 5 character(s)");
    expect(screen.getByTestId("stringLength").innerHTML).toBe("String must contain exactly 5 character(s)");
    expect(screen.getByTestId("stringTooLong").innerHTML).toBe("String must contain at most 5 character(s)");

    expect(screen.getByTestId("numberLessThan").innerHTML).toBe("Number must be less than 5");
    expect(screen.getByTestId("numberLessThanOrEqual").innerHTML).toBe("Number must be less than or equal to 5");
    expect(screen.getByTestId("numberGreaterThan").innerHTML).toBe("Number must be greater than 5");
    expect(screen.getByTestId("numberGreaterThanOrEqual").innerHTML).toBe("Number must be greater than or equal to 5");
    expect(screen.getByTestId("numberMultipleOf").innerHTML).toBe("Number must be a multiple of 5");
  });

  it("should render zod errors in german if language is set to german", () => {
    render(
      <I18nProvider options={{ lng: "de" }}>
        <Content />
      </I18nProvider>,
    );

    expect(screen.getByTestId("undefined").innerHTML).toBe("Darf nicht leer sein");

    expect(screen.getByTestId("email").innerHTML).toBe("Ungültige E-Mail-Adresse");
    expect(screen.getByTestId("url").innerHTML).toBe("Ungültige URL");
    expect(screen.getByTestId("uuid").innerHTML).toBe("Ungültige Kennung");
    expect(screen.getByTestId("guid").innerHTML).toBe("Ungültige Kennung");
    expect(screen.getByTestId("emptyString").innerHTML).toBe("Darf nicht leer sein");
    expect(screen.getByTestId("stringTooShort").innerHTML).toBe("String muss mindestens 5 Zeichen enthalten");
    expect(screen.getByTestId("stringLength").innerHTML).toBe("String muss genau 5 Zeichen enthalten");
    expect(screen.getByTestId("stringTooLong").innerHTML).toBe("String darf höchstens 5 Zeichen enthalten");

    expect(screen.getByTestId("numberLessThan").innerHTML).toBe("Zahl muss kleiner als 5 sein");
    expect(screen.getByTestId("numberLessThanOrEqual").innerHTML).toBe("Zahl muss kleiner oder gleich 5 sein");
    expect(screen.getByTestId("numberGreaterThan").innerHTML).toBe("Zahl muss größer als 5 sein");
    expect(screen.getByTestId("numberGreaterThanOrEqual").innerHTML).toBe("Zahl muss größer oder gleich 5 sein");
    expect(screen.getByTestId("numberMultipleOf").innerHTML).toBe("Zahl muss ein Vielfaches von 5 sein");
  });

  it("should switch the language of zod errors when changing the language in i18next", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider>
        <Content />
      </I18nProvider>,
    );

    expect(screen.getByTestId("undefined").innerHTML).toBe("Required");

    await user.click(screen.getByTestId("set-language"));

    expect(screen.getByTestId("undefined").innerHTML).toBe("Darf nicht leer sein");
  });

  it("should not register dev or large resource bundles", () => {
    function BundleInspector(): React.ReactElement {
      const { i18n } = useTranslation();
      return (
        <>
          <div data-testid="has-dev">{String(i18n.hasResourceBundle("dev", "translation"))}</div>
          <div data-testid="has-large">{String(i18n.hasResourceBundle("large", "translation"))}</div>
        </>
      );
    }

    render(
      <I18nProvider
        languageResources={{
          de: { translation: { key: "Wert" } },
          en: { translation: { key: "Value" } },
        }}
      >
        <BundleInspector />
      </I18nProvider>,
    );

    expect(screen.getByTestId("has-dev").innerHTML).toBe("false");
    expect(screen.getByTestId("has-large").innerHTML).toBe("false");
  });
});
