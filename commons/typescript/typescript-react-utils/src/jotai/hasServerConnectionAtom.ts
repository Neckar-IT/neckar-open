/**
 * Imports
 */

import { atom } from "jotai";

/**
 * Server Connection Atom
 *
 * Stores a boolean to indicate if the backend server is reachable
 */

export const hasServerConnectionAtom = atom<boolean>(true);
