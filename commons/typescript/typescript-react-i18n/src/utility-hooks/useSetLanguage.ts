/**
 * Imports
 */

import { useTranslation } from "react-i18next";

/**
 * Hook to set the language
 *
 * @returns a function to set the language
 */

export function useSetLanguage(): (language: string) => void {
  const { i18n } = useTranslation();

  // Function to set the language.
  const setLanguage = (language: string) => {
    void i18n.changeLanguage(language);
  };

  return setLanguage;
}
