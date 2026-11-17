/**
 * Imports
 */

import { createStore } from "jotai";

/**
 * Jotai- Store
 *
 * This allows access to the store from outside of the react component tree (e.g. jotaiStore.get(clientIdAtom))
 */

export const jotaiStore: ReturnType<typeof createStore> = createStore();
