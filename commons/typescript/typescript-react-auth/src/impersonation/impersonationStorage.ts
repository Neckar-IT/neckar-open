/**
 * Request header naming the user a request acts as. The Ktor side resolves it in
 * `it.neckar.user.management.ktor.registeredUser`.
 */
export const ImpersonationHeader = "NeckarIT-Impersonate";

/**
 * sessionStorage key of the user id this tab acts as. sessionStorage belongs to one tab, so a newly
 * opened tab keeps the signed-in user's own account; a duplicated tab copies the value.
 */
export const ImpersonationStorageKey = "neckarit.impersonation.targetUserId";

/**
 * The id of the user this tab acts as, or `null` while it acts as the signed-in user.
 */
export function readImpersonatedUserId(): string | null {
  return window.sessionStorage.getItem(ImpersonationStorageKey);
}

/**
 * Makes this tab act as [userId]. Opens the start page, so nothing loaded as the previous identity
 * stays on screen.
 */
export function startImpersonation(userId: string): void {
  window.sessionStorage.setItem(ImpersonationStorageKey, userId);
  window.location.assign("/");
}

/**
 * Returns this tab to the signed-in user's own account, opening the start page for the same reason as
 * [startImpersonation].
 */
export function stopImpersonation(): void {
  window.sessionStorage.removeItem(ImpersonationStorageKey);
  window.location.assign("/");
}
