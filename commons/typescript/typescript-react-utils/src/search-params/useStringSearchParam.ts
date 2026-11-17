/**
 * Imports
 */

import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { parseAsString, useQueryState } from "nuqs";

/**
 * Hook to manage a single string search parameter.
 * This hook should not be used for filters (i.e. in tables or lists), use the `useStringFilterSearchParam` hook instead.
 */

export function useStringSearchParam(key: string, prefix?: string) {
  const parsedKey = prefix ? `${prefix}${capitalizeFirstCharacter(key)}` : key;
  const [param, setParam] = useQueryState(parsedKey, parseAsString.withOptions({ history: "push" }));

  const value: string | undefined = param ?? undefined;

  const setValue = (value: string | undefined) => {
    void setParam(value ?? null);
  };

  return [value, setValue] as const;
}
