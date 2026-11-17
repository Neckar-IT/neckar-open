/**
 * Context handed to the auth config's `renderNotRegistered` when GET /me answered
 * `bad/NotRegistered`.
 */
export interface NotRegisteredContext {
  /**
   * Whether registering without an invitation is possible on this service.
   */
  selfSignupAllowed: boolean;

  /**
   * Invalidates the me query so the provider re-evaluates the session — call after a successful
   * registration.
   */
  onRegistered: () => void;

  /**
   * The product's contact address, so a registration flow shows the same address as the provider's
   * own access screens.
   */
  contactEmail?: string;

  /** Ends the session — the provider's own logout, so a registration flow cannot pick a wrong one. */
  onLogout: () => void;

  /** Whether that logout is already on its way. */
  logoutPending: boolean;
}
