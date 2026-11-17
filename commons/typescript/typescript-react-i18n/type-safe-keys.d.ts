import "i18next";

/**
 * Type-safe i18next keys — opt-in augmentation, owned by this package.
 *
 * This is the only `declare module "i18next"` in the monorepo. It is deliberately **excluded** from
 * this package's own `tsconfig` compilation (see `tsconfig.json#exclude`) so the generic, key-agnostic
 * helpers in this library (`useDynamicTranslation`, `ZodErrorTranslationsProvider`, …) keep compiling
 * against unconstrained string keys. It is shipped via the `./type-safe-keys` package export and
 * only enters the program of a frontend that imports it.
 *
 * A frontend opts in with a single declaration file and never references the `"i18next"` module
 * itself — so it needs no direct `i18next` dependency:
 * ```
 * import "@neckar-hub/typescript-react-i18n/type-safe-keys";
 * import type { languageResources } from "./locales/languageResources.ts";
 * type AppI18nKeys = (typeof languageResources)["de"]["translation"];
 * declare global {
 *   interface NeckarI18nKeyRegistry extends AppI18nKeys {}
 * }
 * ```
 *
 * The keys derive automatically from the frontend's `de.json` via `typeof`.
 * A frontend that does not import this augmentation keeps i18next's default loose `string` keys.
 */
declare global {
  interface NeckarI18nKeyRegistry {}
}

declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: "translation";
    resources: {
      translation: NeckarI18nKeyRegistry;
    };
  }
}
