/**
 * Imports
 */

import { atomWithLocalStorage } from "@neckar-hub/typescript-react-utils/jotai";

/**
 * Store the language settings in local storage
 */

interface I18nSettings {
  version: string;
  userLanguages: Record<string, string>;
}

export const i18nSettingsAtom = atomWithLocalStorage<I18nSettings>("i18nSettings", {
  version: "1.0",
  userLanguages: {},
});
