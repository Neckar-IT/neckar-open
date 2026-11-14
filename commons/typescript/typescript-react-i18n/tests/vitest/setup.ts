/**
 * Imports
 */

import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import i18next from "i18next";
import { initReactI18next } from "react-i18next";
import { afterAll, afterEach, beforeEach, vi } from "vitest";

/**
 * Setup Tests
 */

// Initialize i18next with dummy resources so the languages resolve in tests.
beforeEach(async () => {
  await i18next.use(initReactI18next).init({
    resources: {
      en: {
        translation: {
          Cancel: "Cancel",
          Save: "Save",
        },
      },
      de: {
        translation: {
          Cancel: "Abbrechen",
          Save: "Speichern",
        },
      },
    },
    lng: "en",
    interpolation: {
      escapeValue: false,
    },
  });
});

afterEach(() => {
  cleanup();
});

// A worker that does not isolate test files keeps the modules a file mocked in the module cache for the next file.
afterAll(() => {
  vi.resetModules();
});
