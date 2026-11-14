import { type TextKey, type TextKeyWithOptions } from "@neckar-hub/typescript-react-i18n";
import { useEmbeddedTranslation } from "./useEmbeddedTranslation";

interface TranslateEmbeddedProps {
  textKey: TextKey | TextKeyWithOptions;
}

/**
 * Resolves a `TextKey` against the embedded UI-library translation namespace.
 *
 * For internal use within `typescript-react-ui` only. Applications should use the
 * app-level `Translate` from `@neckar-hub/typescript-react-i18n`.
 */
export function TranslateEmbedded({ textKey }: TranslateEmbeddedProps): React.ReactNode {
  const { t } = useEmbeddedTranslation();
  return <>{t(textKey)}</>;
}
