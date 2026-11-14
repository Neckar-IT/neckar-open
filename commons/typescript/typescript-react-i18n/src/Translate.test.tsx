/**
 * Imports
 */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { I18nProvider } from "./I18nProvider";
import { Translate } from "./Translate";
import { useSetLanguage } from "./utility-hooks/useSetLanguage";

/**
 * Test Setup
 */

const languageResources = {
  de: {
    translation: {
      greeting: "Hallo Welt",
      welcome: "Willkommen, {{name}}",
    },
  },
  en: {
    translation: {
      greeting: "Hello World",
      welcome: "Welcome, {{name}}",
    },
  },
};

function LanguageSwitcher(): React.ReactElement {
  const setLanguage = useSetLanguage();
  return (
    <button data-testid="set-de" onClick={() => setLanguage("de")}>
      de
    </button>
  );
}

/**
 * Tests
 */

describe("Translate", () => {
  it("renders the translated string for a plain TextKey", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <div data-testid="translated">
          <Translate textKey="greeting" />
        </div>
        <LanguageSwitcher />
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated").innerHTML).toBe("Hello World");

    await user.click(screen.getByTestId("set-de"));

    expect(screen.getByTestId("translated").innerHTML).toBe("Hallo Welt");
  });

  it("renders the translated string for a TextKeyWithOptions, applying interpolation", () => {
    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <div data-testid="translated">
          <Translate textKey={{ key: "welcome", options: { name: "Robert" } }} />
        </div>
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated").innerHTML).toBe("Welcome, Robert");
  });

  it("returns the key when no translation is found", () => {
    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <div data-testid="translated">
          <Translate textKey="missing.key" />
        </div>
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated").innerHTML).toBe("missing.key");
  });
});
