/**
 * Imports
 */

import type { ResourceKey, TOptions } from "i18next";
import { useCallback } from "react";
import { useTranslation } from "../utility-hooks/useTranslation";

/**
 * Alias Type to signal that the given string will be translated.
 * This is mainly used in packages that bring their onw translations but where a custom translation could be provided.
 */
export type TextKey = string;

export interface TextKeyWithOptions<T = Omit<TOptions, "context"> & { context?: string | undefined }> {
  key: string;
  options: T;
}

/**
 * The key structure of one language file, as a constraint on the other: same keys, same nesting,
 * any value. Applied in both directions it makes the two files hold exactly the same keys.
 */
type KeyMirror<Reference> = {
  [Key in keyof Reference]: Reference[Key] extends object ? KeyMirror<Reference[Key]> : unknown;
};

interface UseEmbeddedTranslationParams<German extends ResourceKey, English extends ResourceKey> {
  namespace: string;
  /**
   * The mirroring runs both ways because a JSON import is not a fresh object literal and so gets no
   * excess-property check: constraining `en` alone would catch a key missing from `en` and let a key
   * missing from `de` through.
   */
  languageResources: {
    de: German & KeyMirror<English>;
    en: English & KeyMirror<German>;
  };
}

/**
 * Hook to be used to embed translations in packages that export components.
 */
export function useInitializeEmbeddedTranslations<German extends ResourceKey, English extends ResourceKey>({
  namespace,
  languageResources,
}: UseEmbeddedTranslationParams<German, English>) {
  const { t, i18n } = useTranslation();

  // i18n instance has to be initialized by the app
  if (!i18n.isInitialized) {
    throw new Error(
      "An i18next instance has to be initialized with the I18nProvider (`internal/open/commons/typescript/typescript-react-utils/i18n`).",
    );
  }

  // load the language resources of the component-library (if not already loaded)
  if (!i18n.hasResourceBundle("en", namespace)) {
    i18n.addResourceBundle("en", namespace, languageResources.en);
  }

  if (!i18n.hasResourceBundle("de", namespace)) {
    i18n.addResourceBundle("de", namespace, languageResources.de);
  }

  const translate = useCallback(
    (textKey: TextKey | TextKeyWithOptions) => {
      let translation: string;

      if (typeof textKey === "object") {
        translation = t([textKey.key, `${namespace}:${textKey.key}`], textKey.options);
      } else {
        translation = t([textKey, `${namespace}:${textKey}`]);
      }

      // On a missing key i18next returns the key itself, which would still carry the namespace prefix; strip it.
      if (translation.startsWith(`${namespace}:`)) {
        translation = translation.replace(`${namespace}:`, "");
      }

      return translation;
    },
    [t, namespace],
  );

  return { t: translate, i18n };
}
