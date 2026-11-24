/**
 * Imports
 */

import { enumTranslationKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import type { VariantProps } from "class-variance-authority";
import type { ToggleGroup as ToggleGroupPrimitive } from "radix-ui";
import type { toggleVariants } from "../../primitives/toggle";
import { ToggleGroup, ToggleGroupItem } from "../../primitives/toggle-group";

/**
 * Prop Types
 */

interface ToggleGroupFromEnumProps<T extends Record<string, string | number>>
  extends Omit<ToggleGroupPrimitive.ToggleGroupSingleProps, "type" | "children">, VariantProps<typeof toggleVariants> {
  enumObject: T;
  /**
   * Name of the enum the items come from, e.g. `"MonitoringSystem"`.
   *
   * Qualifies the translation lookup — see {@link enumTranslationKeys}. Without it the lookup
   * reads the flat `enums.<value>` alone.
   */
  enumName?: string;
}

/**
 * Single-select ToggleGroup that generates one item per value of a TypeScript enum automatically, with i18n labels.
 */

export function ToggleGroupFromEnum<T extends Readonly<Record<string, string | number>>>({
  enumObject,
  enumName,
  ...rest
}: ToggleGroupFromEnumProps<T>): React.ReactElement {
  const { t } = useTranslation();

  return (
    <ToggleGroup type="single" {...rest}>
      {Object.values(enumObject).map((value) => (
        <ToggleGroupItem key={value} value={value.toString()} className="px-4 whitespace-nowrap">
          {t(enumTranslationKeys(value, enumName))}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
