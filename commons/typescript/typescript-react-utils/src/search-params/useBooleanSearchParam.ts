/**
 * Imports
 */

import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { parseAsBoolean, useQueryState } from "nuqs";

/**
 * Hook to manage a single boolean search parameter.
 * This hook should not be used for filters (i.e. in tables or lists), use the `useBooleanFilterSearchParam` hook instead.
 */

export function useBooleanSearchParam(key: string, defaultValue: boolean = false, prefix?: string) {
  const parsedKey = prefix ? `${prefix}${capitalizeFirstCharacter(key)}` : key;
  const [param, setParam] = useQueryState(parsedKey, parseAsBoolean.withDefault(defaultValue).withOptions({ history: "push" }));

  // `param || undefined` would collapse `false` to `undefined` and indistinguishable from "not set".
  // `withDefault` already guarantees a defined value, so we surface it as-is.
  const value: boolean = param;

  const setValue = (value: boolean | undefined) => {
    void setParam(value ?? null);
  };

  return [value, setValue] as const;
}
