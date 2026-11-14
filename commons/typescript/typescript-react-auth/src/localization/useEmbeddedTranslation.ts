import { useInitializeEmbeddedTranslations } from "@neckar-hub/typescript-react-i18n";
import de from "./de.json";
import en from "./en.json";

const languageResources = {
  de,
  en,
};

export function useEmbeddedTranslation() {
  return useInitializeEmbeddedTranslations({ namespace: "auth", languageResources });
}
