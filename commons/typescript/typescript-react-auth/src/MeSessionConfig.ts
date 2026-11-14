import type { MeResponse } from "@neckar-hub/typescript-utils/openapi-types";
import type { ReactElement } from "react";
import type { NotRegisteredContext } from "./NotRegisteredContext";

/**
 * What every auth provider needs regardless of how the session was established: where the identity
 * comes from, and what to render when the identity has no account here.
 */
export interface MeSessionConfig {
  /**
   * Fetches the user from the backend.
   */
  onGetMe: () => Promise<MeResponse>;

  /**
   * Renders the product's registration flow when GET /me answered `bad/NotRegistered`.
   * Without it the provider falls back to the access-denied screen.
   */
  renderNotRegistered?: (context: NotRegisteredContext) => ReactElement;

  /**
   * Address a user without access can write to for an invitation (e.g. `info@neckar.it`).
   * Without it the access screen names no contact at all.
   */
  contactEmail?: string;
}
