/**
 * This sets the option getOnInit = true, which means that the atom will be initialized with the value from
 * localStorage.
 *
 * The default is getOnInit = false, which means that the atom will be initialized with the default value given. This
 * means that only on the second render the atom will have the value from localStorage.
 */

import { atomWithStorage } from "jotai/utils";

export function atomWithLocalStorage<T>(key: string, initialValue: T) {
  return atomWithStorage(key, initialValue, undefined, { getOnInit: true });
}
