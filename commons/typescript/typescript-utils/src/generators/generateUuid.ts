/**
 * Imports
 */

import { v4 } from "uuid";
import type { Uuid } from "../types";

/**
 * generate a new UUID
 */

export function generateUuid(): Uuid {
  return v4();
}
