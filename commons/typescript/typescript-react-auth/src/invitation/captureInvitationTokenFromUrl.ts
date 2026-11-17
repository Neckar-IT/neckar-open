import { InvitationQueryParam, storePendingInvitationToken } from "./pendingInvitationToken";

/**
 * Reads the `?invitation=` token from the current URL; if present, parks it in sessionStorage and
 * removes the param from the URL (via `history.replaceState`, so no navigation occurs).
 *
 * Idempotent and side-effect-safe to call once at module load — it must run before the Keycloak
 * provider triggers a login redirect, otherwise the param would be lost across the round trip.
 */
export function captureInvitationTokenFromUrl(): void {
  const url = new URL(window.location.href);
  const token = url.searchParams.get(InvitationQueryParam);
  if (token === null || token === "") {
    return;
  }

  storePendingInvitationToken(token);

  url.searchParams.delete(InvitationQueryParam);
  window.history.replaceState(window.history.state, "", url.toString());
}
