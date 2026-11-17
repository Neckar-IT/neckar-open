/**
 * Imports
 */

import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { parseAsFloat, useQueryState } from "nuqs";

/**
 * Hook to manage a single decimal search parameter.
 * This hook should not be used for filters (i.e. in tables or lists), use the `useFloatFilterSearchParam` hook instead.
 */

export function useFloatSearchParam(key: string, defaultValue?: number, prefix?: string) {
  const parsedKey = prefix ? `${prefix}${capitalizeFirstCharacter(key)}` : key;
  const [param, setParam] = useQueryState(
    parsedKey,
    isDefined(defaultValue)
      ? parseAsFloat.withDefault(defaultValue).withOptions({ history: "push" })
      : parseAsFloat.withOptions({ history: "push" }),
  );

  const value: number | undefined = param ?? undefined;

  const setValue = (value: number | undefined) => {
    void setParam(value ?? null);
  };

  return [value, setValue] as const;
}
