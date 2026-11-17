/**
 * Imports
 */

import { isDevelopmentStage } from "@neckar-hub/typescript-utils/runtime-context";
import { useTranslation } from "./useTranslation";

/**
 * Escape hatch for intentionally dynamic, open-vocabulary translation keys — e.g.
 * `enums.${freeBackendString}` where the value set is unbounded and cannot be a literal union.
 *
 * Returns a `t`-like function that takes a plain `string`, bypassing the compile-time key check
 * that a consuming app's i18next module augmentation (its `i18n.d.ts`) adds to `t("…")` — this hook
 * lives in the commons library, which is not itself key-typed. Use this ONLY when the key vocabulary
 * is genuinely open; prefer a statically typed key (literal union) wherever the set is closed,
 * because that keeps full type safety and even flags missing translations at compile time.
 *
 * On a development stage it warns when a dynamic key has no translation (i18next would otherwise
 * render the raw key, e.g. `enums.Discover`) — on the deployed dev host too, not just in a local
 * dev server. Calls to this hook are greppable (`useDynamicTranslation`), so every deliberate
 * bypass of the key typing stays auditable.
 */
export function useDynamicTranslation(): (key: string) => string {
  const { t, i18n } = useTranslation();

  return (key: string): string => {
    if (isDevelopmentStage() && !i18n.exists(key)) {
      console.warn(`[i18n] Missing translation for dynamic key: "${key}"`);
    }
    return t(key);
  };
}
