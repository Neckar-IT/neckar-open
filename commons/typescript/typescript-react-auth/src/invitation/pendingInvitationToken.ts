/**
 * sessionStorage key under which a pending invitation token is parked across the Keycloak login
 * redirect. The token is captured from the `?invitation=` URL param before login and replayed as
 * the [InvitationTokenHeader] on every request until registration completes.
 */
export const PendingInvitationTokenKey = "pendingInvitationToken";

/**
 * Query parameter that carries the invitation token into an app (built by the inviter as
 * `${window.location.origin}/?invitation=${token}`).
 */
export const InvitationQueryParam = "invitation";

/**
 * Request header the parked token is replayed as. The Ktor side reads it in
 * `it.neckar.user.management.invitation.InvitationTokenPlugin` and must allow it through CORS.
 */
export const InvitationTokenHeader = "NeckarIT-Invitation-Token";

/**
 * Reads the currently parked invitation token, or `null` if none is stored.
 */
export function readPendingInvitationToken(): string | null {
  return window.sessionStorage.getItem(PendingInvitationTokenKey);
}

/**
 * Parks an invitation token in sessionStorage so it survives the Keycloak login redirect.
 */
export function storePendingInvitationToken(token: string): void {
  window.sessionStorage.setItem(PendingInvitationTokenKey, token);
}

/**
 * Clears the parked invitation token. Called once registration has completed.
 */
export function clearPendingInvitationToken(): void {
  window.sessionStorage.removeItem(PendingInvitationTokenKey);
}
