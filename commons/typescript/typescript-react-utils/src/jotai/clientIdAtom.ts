/**
 * Imports
 */

import { generateNanoId } from "@neckar-hub/typescript-utils/generators";
import { atom } from "jotai";

/**
 * Client ID Atom
 *
 * This atom is used to store a client Id for a session that can be used to identify requests from a client
 */

const clientId = generateNanoId(10);

export const clientIdAtom = atom<string>(clientId);
