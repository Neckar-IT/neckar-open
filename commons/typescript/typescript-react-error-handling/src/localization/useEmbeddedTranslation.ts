import { useInitializeEmbeddedTranslations } from "@neckar-hub/typescript-react-i18n";
import de from "./de.json";
import en from "./en.json";

/** Namespace of this package's bundle, for components that resolve keys outside the hook's `t`, such as `<Trans>`. */
export const EmbeddedTranslationNamespace = "error-handling";

const languageResources = {
  de,
  en,
};

export function useEmbeddedTranslation() {
  return useInitializeEmbeddedTranslations({ namespace: EmbeddedTranslationNamespace, languageResources });
}
