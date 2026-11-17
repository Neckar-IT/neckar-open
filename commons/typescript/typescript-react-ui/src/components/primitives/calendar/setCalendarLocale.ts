/**
 * Imports
 */

import { de, enUS, type Locale } from "date-fns/locale";

/**
 * Detect i18next language and set date-fns locale accordingly
 */

export function setCalendarLocale(detectedLanguage: string): Locale {
  // `split` always yields at least one element — the default only satisfies the type checker.
  const [languageCode = detectedLanguage] = detectedLanguage.split("-");

  switch (languageCode) {
    case "de":
      return de;
    case "en":
      return enUS;
    default:
      // Intentional fallback: the calendar must keep working for all i18next languages.
      // Add a case above to support a new language with a localized date-fns locale.
      console.warn(
        `No locale found for Calendar Component for language code "${detectedLanguage}". Using "en" as fallback. You can extend the available calendar languages in the component library.`,
      );
      return enUS;
  }
}
