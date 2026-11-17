import { useInitializeEmbeddedTranslations } from "@neckar-hub/typescript-react-i18n";
import de from "./de.json";
import en from "./en.json";

const languageResources = {
  de,
  en,
};

/** The namespace `<Trans>` needs, since it resolves keys outside the embedded `t`. */
export const embeddedTranslationNamespace = "ui-components";

export function useEmbeddedTranslation() {
  return useInitializeEmbeddedTranslations({ namespace: embeddedTranslationNamespace, languageResources });
}
