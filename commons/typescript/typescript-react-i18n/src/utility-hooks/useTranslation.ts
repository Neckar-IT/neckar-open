/**
 * Imports
 */

import { isDevelopmentStage } from "@neckar-hub/typescript-utils/runtime-context";
import { useTranslation as useReactI18nextTranslation } from "react-i18next";
import { transformDev } from "../dev-languages/transformDev";
import { transformLarge } from "../dev-languages/transformLarge";

/**
 * Neckar wrapper around react-i18next's `useTranslation`.
 *
 * On a development stage, when `i18n.language === "dev"` or `"large"`, the returned `t`
 * function applies the pseudo-language transform on every call — so QA modes
 * also catch translations registered dynamically via `i18n.addResourceBundle`
 * (embedded translations from component libraries), which pre-generated
 * resource bundles could never reach.
 *
 * Everywhere else, returns the react-i18next response unchanged.
 *
 * The stage is read per call, not cached in a module constant. `useTranslation` does run in every
 * component on every render, but the read is one property lookup plus a lookup in a six-element
 * list — nothing against what React does per render — and caching it would make the case that
 * actually matters untestable: that these modes are off anywhere but a development stage.
 */

type UseTranslationType = typeof useReactI18nextTranslation;
type I18nextResponse = ReturnType<UseTranslationType>;
type TranslationFunction = I18nextResponse["t"];

export const useTranslation: UseTranslationType = function useTranslation(...args: Parameters<UseTranslationType>): I18nextResponse {
  const response = useReactI18nextTranslation(...args);
  if (!isDevelopmentStage()) {
    return response;
  }
  const { t: originalT, i18n, ready } = response;
  const language = i18n.language;
  switch (language) {
    case "dev":
    case "large":
      break;
    default:
      return response;
  }
  const wrappedT = wrapTranslationFunction(originalT, language);
  const tuple: [TranslationFunction, I18nextResponse["i18n"], boolean] = [wrappedT, i18n, ready];
  return Object.assign(tuple, { t: wrappedT, i18n, ready });
};

function wrapTranslationFunction(originalT: TranslationFunction, language: "dev" | "large"): TranslationFunction {
  return new Proxy(originalT, {
    apply(target, thisArg, args): unknown {
      const result = Reflect.apply(target, thisArg, args);
      if (typeof result !== "string") {
        return result;
      }
      if (language === "dev") {
        const [key] = args;
        const firstKey = Array.isArray(key) ? key[0] : key;
        return typeof firstKey === "string" ? transformDev(firstKey) : result;
      }
      return transformLarge(result);
    },
  });
}
