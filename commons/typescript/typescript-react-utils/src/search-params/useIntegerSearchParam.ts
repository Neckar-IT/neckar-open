/**
 * Imports
 */

import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { parseAsInteger, useQueryState } from "nuqs";

/**
 * Hook to manage a single integer search parameter.
 * This hook should not be used for filters (i.e. in tables or lists), use the `useIntegerFilterSearchParam` hook instead.
 */

export function useIntegerSearchParam(key: string, defaultValue?: number, prefix?: string) {
  const parsedKey = prefix ? `${prefix}${capitalizeFirstCharacter(key)}` : key;
  const [param, setParam] = useQueryState(
    parsedKey,
    isDefined(defaultValue)
      ? parseAsInteger.withDefault(defaultValue).withOptions({ history: "push" })
      : parseAsInteger.withOptions({ history: "push" }),
  );

  const value: number | undefined = param ?? undefined;

  const setValue = (value: number | undefined) => {
    void setParam(value ?? null);
  };

  return [value, setValue] as const;
}
