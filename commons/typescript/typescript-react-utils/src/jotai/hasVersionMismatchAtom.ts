/**
 * Imports
 */

import { atom } from "jotai";

/**
 * Version Mismatch Atom
 *
 * Stores a boolean to indicate if there is a version mismatch between the client and server
 */

export const hasVersionMismatchAtom = atom<boolean>(false);
