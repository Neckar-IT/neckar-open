/**
 * Imports
 */

import { isDevelopmentStage } from "@neckar-hub/typescript-utils/runtime-context";
import { createInstance, type FallbackLng, type InitOptions, type Resource } from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import type { PropsWithChildren, ReactElement } from "react";
import { useEffect, useMemo } from "react";
import { I18nextProvider, initReactI18next } from "react-i18next";
import zodErrorsDe from "./locales/de/zod-errors.json";
import zodErrorsEn from "./locales/en/zod-errors.json";
import { ZodErrorTranslationsProvider } from "./ZodErrorTranslationsProvider";

/**
 * I18n Provider
 */

type I18nProviderProps = PropsWithChildren<{
  languageResources?: Resource | undefined;
  options?: Omit<InitOptions, "resources"> | undefined;
}>;

const DEFAULT_FALLBACK_LANGUAGE = "en";
const DEV_LARGE_BASE_LANGUAGE_DEFAULT = "de";

export function I18nProvider(props: I18nProviderProps): ReactElement {
  // initialize i18next
  const i18next = useMemo(() => {
    const resources: Resource = {
      ...props.languageResources,
      de: { zod: zodErrorsDe, ...props.languageResources?.de },
      en: { zod: zodErrorsEn, ...props.languageResources?.en },
    };

    const instance = createInstance({
      fallbackLng: resolveFallbackLng(props.options),
      resources,
      interpolation: {
        escapeValue: false,
      },
      defaultNS: "translation",
      detection: {
        order: ["navigator"],
        caches: [],
      },
      ...props.options,
    });

    void instance.use(initReactI18next).use(LanguageDetector).init();

    return instance;
  }, [props.languageResources, props.options]);

  // we need to set the html lang attribute for hyphenation to work properly
  useEffect(() => {
    window.document.documentElement.lang = i18next.language;

    const handleLanguageChange = (lng: string) => {
      window.document.documentElement.lang = lng;
    };

    i18next.on("languageChanged", handleLanguageChange);
    return () => {
      i18next.off("languageChanged", handleLanguageChange);
    };
  }, [i18next]);

  return (
    <I18nextProvider i18n={i18next}>
      <ZodErrorTranslationsProvider>{props.children}</ZodErrorTranslationsProvider>
    </I18nextProvider>
  );
}

/**
 * Builds the i18next `fallbackLng` config.
 *
 * On a development stage, maps the pseudo-languages `dev` and `large` to the base
 * application language so that i18next can still resolve real resource values.
 * The call-site wrappers (`useTranslation`, `useInitializeEmbeddedTranslations`)
 * then transform those values into `[key]` / `«inflated»` on the fly. Gated on the same stage as
 * those wrappers — a mapping without the transform, or the other way round, would be half a QA mode.
 *
 * Everywhere else, returns the user-supplied `fallbackLng` (or `"en"` default) unchanged.
 */
function resolveFallbackLng(options: InitOptions | undefined): false | FallbackLng {
  const userFallback = options?.fallbackLng;
  if (!isDevelopmentStage()) {
    return userFallback ?? DEFAULT_FALLBACK_LANGUAGE;
  }

  // Only augment simple fallbacks (string, string[]) with dev/large mapping. An already-complex
  // fallback (object map, resolver function) is passed through as-is — mixing it with our
  // dev/large entries would produce ambiguous behavior.
  const simpleFallback = toSimpleFallback(userFallback);
  if (simpleFallback === undefined) {
    return userFallback ?? DEFAULT_FALLBACK_LANGUAGE;
  }

  const firstSimple = simpleFallback.length > 0 ? simpleFallback[0] : undefined;
  const baseLanguage = options?.lng ?? firstSimple ?? DEV_LARGE_BASE_LANGUAGE_DEFAULT;
  return {
    dev: [baseLanguage],
    large: [baseLanguage],
    default: simpleFallback,
  };
}

function toSimpleFallback(userFallback: false | FallbackLng | undefined): string[] | undefined {
  if (userFallback === undefined || userFallback === false) {
    return [DEFAULT_FALLBACK_LANGUAGE];
  }
  if (typeof userFallback === "string") {
    return [userFallback];
  }
  if (Array.isArray(userFallback)) {
    return [...userFallback];
  }
  return undefined;
}
