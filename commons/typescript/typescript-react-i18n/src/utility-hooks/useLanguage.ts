/**
 * Imports
 */

import { useTranslation } from "react-i18next";

/**
 * useLanguage Hook
 *
 * @returns {string} The currently selected language key (e.g. "en", "de")
 */

export function useLanguage(): string {
  const { i18n } = useTranslation();

  // The browser-preferred language, even when no translations exist for it, so numbers and dates follow that locale
  // while text falls back to "en".
  return i18n.language;
}
