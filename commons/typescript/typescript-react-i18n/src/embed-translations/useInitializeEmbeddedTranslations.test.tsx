/**
 * Imports
 */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { I18nProvider } from "../I18nProvider";
import { useSetLanguage } from "../utility-hooks/useSetLanguage";
import type { TextKey } from "./useInitializeEmbeddedTranslations";
import { useInitializeEmbeddedTranslations } from "./useInitializeEmbeddedTranslations";

/**
 * Embedded Language Resources and Translation Resources
 */

const languageResources = {
  de: {
    test: {
      key: "Das ist ein Test",
    },
  },
  en: {
    test: {
      key: "This is a test",
    },
  },
};

function useEmbeddedTranslation() {
  return useInitializeEmbeddedTranslations({ namespace: "test", languageResources });
}

/**
 * Test Setup
 */

function Component({ textKey = "test.key" }: { textKey?: TextKey }): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const setLanguage = useSetLanguage();

  return (
    <>
      <div data-testid="translated-text">{t(textKey)}</div>
      <button onClick={() => setLanguage("de")}>Set Language</button>
    </>
  );
}

/**
 * Tests
 */

describe("useEmbeddedTranslation", () => {
  it("should use the embedded language resources for translations by default", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider>
        <Component />
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated-text").innerHTML).toBe("This is a test");
    await user.click(screen.getByRole("button"));
    expect(screen.getByTestId("translated-text").innerHTML).toBe("Das ist ein Test");
  });

  it("should use the global language resources for translations if provided", async () => {
    const user = userEvent.setup();

    const globalLanguageResources = {
      de: {
        translation: {
          someKey: "Ein globaler Text",
        },
      },
      en: {
        translation: {
          someKey: "A global text",
        },
      },
    };

    render(
      <I18nProvider languageResources={globalLanguageResources}>
        <Component textKey="someKey" />
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated-text").innerHTML).toBe("A global text");
    await user.click(screen.getByRole("button"));
    expect(screen.getByTestId("translated-text").innerHTML).toBe("Ein globaler Text");
  });

  it("should return the text key if no translation is found", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider>
        <Component textKey="Diesen Schlüssel gibt es nicht." />
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated-text").innerHTML).toBe("Diesen Schlüssel gibt es nicht.");
    await user.click(screen.getByRole("button"));
    expect(screen.getByTestId("translated-text").innerHTML).toBe("Diesen Schlüssel gibt es nicht.");
  });

  it("should render the key itself when one language file misses it, which the type rejects", () => {
    const asymmetricLanguageResources = {
      de: {
        test: {
          key: "Das ist ein Test",
          nurAufDeutsch: "Nur auf Deutsch",
        },
      },
      en: {
        test: {
          key: "This is a test",
        },
      },
    };

    function AsymmetricComponent(): React.ReactElement {
      const { t } = useInitializeEmbeddedTranslations({
        namespace: "asymmetric",
        // @ts-expect-error `en` misses `test.nurAufDeutsch`, which `de` defines — this is what the mirrored type is for
        languageResources: asymmetricLanguageResources,
      });

      return <div data-testid="translated-text">{t("test.nurAufDeutsch")}</div>;
    }

    render(
      <I18nProvider>
        <AsymmetricComponent />
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated-text").innerHTML).toBe("test.nurAufDeutsch");
  });

  it("should transform embedded translations in dev mode", async () => {
    const user = userEvent.setup();

    function DevModeComponent(): React.ReactElement {
      const { t } = useEmbeddedTranslation();
      const setLanguage = useSetLanguage();

      return (
        <>
          <div data-testid="translated-text">{t("test.key")}</div>
          <button onClick={() => setLanguage("dev")}>Set Language</button>
        </>
      );
    }

    render(
      <I18nProvider>
        <DevModeComponent />
      </I18nProvider>,
    );

    await user.click(screen.getByRole("button"));

    expect(screen.getByTestId("translated-text").innerHTML).toBe("[test.key]");
  });

  it("should transform embedded translations in large mode", async () => {
    const user = userEvent.setup();

    function LargeModeComponent(): React.ReactElement {
      const { t } = useEmbeddedTranslation();
      const setLanguage = useSetLanguage();

      return (
        <>
          <div data-testid="translated-text">{t("test.key")}</div>
          <button onClick={() => setLanguage("large")}>Set Language</button>
        </>
      );
    }

    render(
      <I18nProvider>
        <LargeModeComponent />
      </I18nProvider>,
    );

    await user.click(screen.getByRole("button"));

    const value = screen.getByTestId("translated-text").innerHTML;
    expect(value.startsWith("«")).toBe(true);
    expect(value.endsWith("»")).toBe(true);
    expect(value.slice(1, -1)).toMatch(/^[ÅŴÑgjp]+$/);
  });
});
