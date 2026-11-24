/**
 * Namespace every enum-value label lives in.
 */
const ENUM_TRANSLATION_PREFIX = "enums";

/**
 * Builds the lookup keys for the label of a single enum value, most specific first.
 *
 * Passing [enumName] puts `enums.<EnumName>.<Value>` in front of the flat key; i18next takes the
 * first key that resolves. The flat key serves products whose labels are still flat.
 *
 * @param value the enum value, e.g. `"Optional"`
 * @param enumName the name of the enum the value belongs to, e.g. `"ConfiguredOptionality"`
 */
export function enumTranslationKeys(value: string | number, enumName?: string): string[] {
  const flatKey = `${ENUM_TRANSLATION_PREFIX}.${value.toString()}`;

  if (enumName === undefined) {
    return [flatKey];
  }

  return [`${ENUM_TRANSLATION_PREFIX}.${enumName}.${value.toString()}`, flatKey];
}
