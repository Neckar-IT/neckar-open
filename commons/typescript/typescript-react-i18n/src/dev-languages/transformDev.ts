/**
 * Dev-language transformation: replaces the final translated string with the
 * translation key wrapped in brackets.
 *
 * Applied by the Neckar `useTranslation` / `useInitializeEmbeddedTranslations`
 * wrappers on every `t(key, …)` call when `i18n.language === "dev"`.
 * Hard-coded strings that bypass i18n never pass through this transform and
 * therefore appear without brackets — which makes missing-translation sites
 * immediately recognizable.
 *
 * Interpolation values are dropped intentionally: Dev mode exists to show the
 * key path, not to render the message.
 */
export function transformDev(key: string): string {
  return `[${key}]`;
}
