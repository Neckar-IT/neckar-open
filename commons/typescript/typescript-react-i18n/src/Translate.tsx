import type { TextKey, TextKeyWithOptions } from "./embed-translations/useInitializeEmbeddedTranslations";
import { useTranslation } from "./utility-hooks/useTranslation";

interface TranslateProps {
  textKey: TextKey | TextKeyWithOptions;
}

/**
 * App-level component that resolves a `TextKey` against the application's i18next instance.
 *
 * For translations bundled with a component library, use `TranslateEmbedded` from the
 * library that owns the translations instead.
 */
export function Translate({ textKey }: TranslateProps): React.ReactNode {
  const { t } = useTranslation();
  if (typeof textKey === "object") {
    return <>{t(textKey.key, textKey.options)}</>;
  }
  return <>{t(textKey)}</>;
}
