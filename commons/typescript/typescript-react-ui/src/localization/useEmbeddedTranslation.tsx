import { useInitializeEmbeddedTranslations } from "@neckar-hub/typescript-react-i18n";
import de from "./de.json";
import en from "./en.json";

const languageResources = {
  de,
  en,
};

/** The namespace a key names when something other than the embedded `t` resolves it: `<Trans>` or the caller's own `t`. */
export const embeddedTranslationNamespace = "ui-components";

export function useEmbeddedTranslation() {
  return useInitializeEmbeddedTranslations({ namespace: embeddedTranslationNamespace, languageResources });
}
