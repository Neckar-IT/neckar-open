/**
 * sessionStorage key holding the browser tab's OTel session ID.
 *
 * Declared once: {@link getOrCreateSessionId} writes it during OTel
 * initialization, {@link readSessionId} reads it for every outgoing request.
 */
const SESSION_ID_STORAGE_KEY = "otel_session_id";

/**
 * Get or create a session ID for the current browser tab.
 *
 * Uses sessionStorage so the ID survives page reloads within the same tab
 * but is discarded when the tab is closed.
 */
export function getOrCreateSessionId(): string {
  let sessionId = window.sessionStorage.getItem(SESSION_ID_STORAGE_KEY);
  if (sessionId == null) {
    sessionId = window.crypto.randomUUID();
    window.sessionStorage.setItem(SESSION_ID_STORAGE_KEY, sessionId);
  }
  return sessionId;
}

/**
 * Read the current browser tab's session ID, or `null` when OTel has not been
 * initialized yet and the ID has therefore never been created.
 */
export function readSessionId(): string | null {
  return window.sessionStorage.getItem(SESSION_ID_STORAGE_KEY);
}
