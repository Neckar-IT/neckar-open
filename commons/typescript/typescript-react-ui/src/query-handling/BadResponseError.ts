import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { RestResponseBad } from "@neckar-hub/typescript-utils/rest";

/**
 * Discriminator string of a REST `BadResponse`, i.e. any `"bad/<Subtype>"` value.
 * Single source for the template form: derived from the contract type in typescript-utils.
 */
export type BadResponseType = RestResponseBad["type"];

/**
 * Error thrown when a server request returns `{ type: "bad/..." }`.
 *
 * This usually indicates that the request was processed by the server (HTTP 2xx),
 * but the business-level outcome was negative. For example, when an entity could
 * not be found in the database, the server responds with `{ type: "bad/NotFound" }`.
 */
export class BadResponseError extends Error {
  readonly badType: BadResponseType;
  /**
   * A descriptive label or i18n key for the entity (e.g., "Zusatzinstallation").
   * Used by `BadResponseFallback` to show a precise NotFound message.
   */
  readonly entityLabel: TextKey | undefined;

  constructor({ badType, message, entityLabel }: { badType: BadResponseType; message?: string; entityLabel?: TextKey }) {
    super(message);
    this.badType = badType;
    this.entityLabel = entityLabel;
    this.name = "BadResponseError";
  }
}
