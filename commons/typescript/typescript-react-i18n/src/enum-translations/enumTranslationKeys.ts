/**
 * Namespace every enum-value label lives in.
 */
const ENUM_TRANSLATION_PREFIX = "enums";

/**
 * Builds the lookup keys for the label of a single enum value, most specific first.
 *
 * Enum labels share one flat namespace: `enums.Selected`, `enums.None`, `enums.Optional`. That reads
 * well as long as a value name means the same thing everywhere — and it stops working the moment two
 * enums disagree. `MonitoringSystem.None` means "no online monitoring" while `enums.None` was
 * already taken by "without cable", and `ConfiguredOptionality.Optional` means "offer as an option"
 * while `enums.Optional` says plain "optional". The dropdown then showed the other enum's label.
 *
 * Passing [enumName] adds `enums.<EnumName>.<Value>` in front of the flat key. i18next takes the
 * first key that resolves, so a qualified entry wins where it exists and every value without one
 * keeps the shared label. Only the colliding values need an entry — the other 113 keys stay put.
 *
 * @param value the enum value, e.g. `"None"`
 * @param enumName the name of the enum the value belongs to, e.g. `"MonitoringSystem"`
 */
export function enumTranslationKeys(value: string | number, enumName?: string): string[] {
  const flatKey = `${ENUM_TRANSLATION_PREFIX}.${value.toString()}`;

  if (enumName === undefined) {
    return [flatKey];
  }

  return [`${ENUM_TRANSLATION_PREFIX}.${enumName}.${value.toString()}`, flatKey];
}
