/**
 * Imports
 */

import { generateNanoId } from "@neckar-hub/typescript-utils/generators";
import { atom } from "jotai";
import { jotaiStore } from "../jotai";

/**
 * Client ID Atom
 */

const clientId = generateNanoId(10);

const clientIdAtom = atom<string>(clientId);

/**
 * Getter Function
 */

export function getClientId() {
  return jotaiStore.get(clientIdAtom);
}
