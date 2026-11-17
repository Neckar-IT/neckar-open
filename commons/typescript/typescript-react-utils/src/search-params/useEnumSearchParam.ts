/**
 * Imports
 */

import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { parseAsStringEnum, useQueryState } from "nuqs";

/**
 * Hook to manage a single enum search parameter.
 * This hook should not be used for filters (i.e. in tables or lists), use the `useEnumFilterSearchParam` hook instead.
 *
 * @param enumValues - The const object containing all enum values (e.g., `ConfirmationStatus`)
 * @param key - The key of the search parameter
 * @param defaultValue - Optional default value; the URL parameter is omitted while the state equals it
 * @param prefix - Optional prefix for the URL keys
 */

export function useEnumSearchParam<T extends Record<string, string>>(
  enumValues: T,
  key: string,
  defaultValue: T[keyof T],
  prefix?: string,
): readonly [T[keyof T], (value: T[keyof T] | undefined) => void];
export function useEnumSearchParam<T extends Record<string, string>>(
  enumValues: T,
  key: string,
  defaultValue?: undefined,
  prefix?: string,
): readonly [T[keyof T] | undefined, (value: T[keyof T] | undefined) => void];
export function useEnumSearchParam<T extends Record<string, string>>(
  enumValues: T,
  key: string,
  defaultValue?: T[keyof T],
  prefix?: string,
) {
  const parsedKey = prefix ? `${prefix}${capitalizeFirstCharacter(key)}` : key;
  const parser = parseAsStringEnum<T[keyof T]>(Object.values(enumValues) as T[keyof T][]);
  const [param, setParam] = useQueryState(
    parsedKey,
    isDefined(defaultValue) ? parser.withDefault(defaultValue).withOptions({ history: "push" }) : parser.withOptions({ history: "push" }),
  );

  const value: T[keyof T] | undefined = param ?? undefined;

  const setValue = (value: T[keyof T] | undefined) => {
    void setParam(value ?? null);
  };

  return [value, setValue] as const;
}
