/**
 * Imports
 */

import { enumTranslationKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import { Select, SelectItem, type SelectProps } from "../select";

/**
 * Prop Types
 */

interface SelectFromEnumProps<T extends Record<string, string | number>> extends Omit<SelectProps, "children"> {
  enumObject: T;
  /**
   * Name of the enum the options come from, e.g. `"MonitoringSystem"`.
   *
   * Qualifies the translation lookup — see {@link enumTranslationKeys}. Pass it whenever a value
   * name of this enum also occurs in another one; leaving it out keeps the shared flat key.
   */
  enumName?: string;
}

/**
 * Select dropdown that generates its options automatically from a TypeScript enum, with i18n labels.
 */

export function SelectFromEnum<T extends Readonly<Record<string, string | number>>>({
  enumObject,
  enumName,
  ...rest
}: SelectFromEnumProps<T>): React.ReactElement {
  const { t } = useTranslation();

  return (
    <Select {...rest}>
      {Object.values(enumObject).map((value) => (
        <SelectItem key={value} value={value.toString()}>
          {t(enumTranslationKeys(value, enumName))}
        </SelectItem>
      ))}
    </Select>
  );
}
