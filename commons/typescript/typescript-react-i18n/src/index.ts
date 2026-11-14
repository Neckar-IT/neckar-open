/**
 * Reexport i18next and react-i18next functionalities
 */
import i18next from "i18next";

export { initReactI18next, Trans } from "react-i18next";
export { i18next };
export type { ParseKeys, TFunction } from "i18next";
export { useTranslation } from "./utility-hooks/useTranslation";

/**
 * Export custom hooks and components
 */
export {
  useInitializeEmbeddedTranslations,
  type TextKey,
  type TextKeyWithOptions,
} from "./embed-translations/useInitializeEmbeddedTranslations";
export { useCurrencyFormatter, type UseCurrencyFormatterOptions } from "./formatter-hooks/useCurrencyFormatter";
export { useDateTimeFormatter } from "./formatter-hooks/useDateTimeFormatter";
export { useFileSizeFormatter } from "./formatter-hooks/useFileSizeFormatter";
export { useHoursAndMinutesFormatter } from "./formatter-hooks/useHoursAndMinutesFormatter";
export { useMaxFractionDigitsFormatter } from "./formatter-hooks/useMaxFractionDigitsFormatter";
export { useNumberFormatter } from "./formatter-hooks/useNumberFormatter";
export { usePercentageFormatter } from "./formatter-hooks/usePercentageFormatter";
export { useRelativeTimeFormatter } from "./formatter-hooks/useRelativeTimeFormatter";
export { useValidFromDateTimeFormatter } from "./formatter-hooks/useValidFromDateTimeFormatter";
export { I18nProvider } from "./I18nProvider";
export { Translate } from "./Translate";
export { useNumberParser } from "./parser-hooks/useNumberParser";
export { UserLanguageProvider } from "./UserLanguageProvider";
export { useLanguage } from "./utility-hooks/useLanguage";
export { useDecimalSeparator, useNumberSeparators, type NumberSeparators } from "./utility-hooks/useNumberSeparators";
export { useSetLanguage } from "./utility-hooks/useSetLanguage";
export { useDynamicTranslation } from "./utility-hooks/useDynamicTranslation";
export { capabilityTranslationKey } from "./capability-translations/capabilityTranslationKey";
export { enumTranslationKeys } from "./enum-translations/enumTranslationKeys";
export { createGermanI18n } from "./german-instance/createGermanI18n";
