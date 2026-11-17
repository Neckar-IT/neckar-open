/**
 * Imports
 */

import { RuntimeContextMetaNames } from "@neckar-hub/typescript-utils/runtime-context";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { I18nProvider } from "../I18nProvider";
import { useSetLanguage } from "./useSetLanguage";
import { useTranslation } from "./useTranslation";

/**
 * Helpers
 */

/**
 * Puts the frontend on a deployment stage, the way the nginx entrypoint does at container start.
 * Without this a vitest run reads as `Development`, which is what the other tests here rely on.
 */
function serveOnStage(deploymentStage: string) {
  const meta = document.createElement("meta");
  meta.setAttribute("name", RuntimeContextMetaNames.deploymentStage);
  meta.setAttribute("content", deploymentStage);
  document.head.appendChild(meta);
}

afterEach(() => {
  document.head.replaceChildren();
});

/**
 * Test Setup
 */

const languageResources = {
  de: {
    translation: {
      greeting: "Hallo Welt",
    },
  },
  en: {
    translation: {
      greeting: "Hello World",
    },
  },
};

function Translated(): React.ReactElement {
  const { t } = useTranslation();
  const setLanguage = useSetLanguage();

  return (
    <>
      <div data-testid="translated">{t("greeting")}</div>
      <button data-testid="set-de" onClick={() => setLanguage("de")}>
        de
      </button>
      <button data-testid="set-dev" onClick={() => setLanguage("dev")}>
        dev
      </button>
      <button data-testid="set-large" onClick={() => setLanguage("large")}>
        large
      </button>
    </>
  );
}

/**
 * Tests
 */

describe("useTranslation", () => {
  it("should pass translations through for real languages", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <Translated />
      </I18nProvider>,
    );

    expect(screen.getByTestId("translated").innerHTML).toBe("Hello World");

    await user.click(screen.getByTestId("set-de"));

    expect(screen.getByTestId("translated").innerHTML).toBe("Hallo Welt");
  });

  it("should return [key] in dev mode", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <Translated />
      </I18nProvider>,
    );

    await user.click(screen.getByTestId("set-dev"));

    expect(screen.getByTestId("translated").innerHTML).toBe("[greeting]");
  });

  it("should inflate the translated value in large mode", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <Translated />
      </I18nProvider>,
    );

    await user.click(screen.getByTestId("set-large"));

    const value = screen.getByTestId("translated").innerHTML;
    expect(value.startsWith("«")).toBe(true);
    expect(value.endsWith("»")).toBe(true);
    expect(value.slice(1, -1)).toMatch(/^[ÅŴÑgjp]+$/);
  });

  it("should resolve dev/large values against the base language via fallbackLng", async () => {
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "de" }}>
        <Translated />
      </I18nProvider>,
    );

    // In large mode, the inflated length is derived from the DE value ("Hallo Welt", 10 chars → 12 + ceil(10 * 1.5) = 27)
    await user.click(screen.getByTestId("set-large"));

    const value = screen.getByTestId("translated").innerHTML;
    expect(value.slice(1, -1).length).toBe(27);
  });

  //The QA modes are a development tool. Whether they are OFF everywhere else is the case that
  //matters - a pseudo-language reaching a customer would replace every label with [key].
  it("should leave the pseudo-languages inert on a production stage", async () => {
    serveOnStage("Production");
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <Translated />
      </I18nProvider>,
    );

    await user.click(screen.getByTestId("set-dev"));

    //No transform, and no dev/large fallback mapping either: i18next falls back to the default
    //language rather than rendering [greeting]
    expect(screen.getByTestId("translated").innerHTML).toBe("Hello World");
  });

  it("should leave the pseudo-languages inert on a demo stage", async () => {
    serveOnStage("Demo");
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <Translated />
      </I18nProvider>,
    );

    await user.click(screen.getByTestId("set-large"));

    expect(screen.getByTestId("translated").innerHTML).toBe("Hello World");
  });

  it("should keep them working on a deployed development stage", async () => {
    serveOnStage("Development");
    const user = userEvent.setup();

    render(
      <I18nProvider languageResources={languageResources} options={{ lng: "en" }}>
        <Translated />
      </I18nProvider>,
    );

    await user.click(screen.getByTestId("set-dev"));

    expect(screen.getByTestId("translated").innerHTML).toBe("[greeting]");
  });
});
