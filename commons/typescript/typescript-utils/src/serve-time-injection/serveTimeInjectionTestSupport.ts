/**
 * Test support for the serve-time injection channel: writes and removes the `<meta>` tag the nginx
 * entrypoint would have substituted at container start.
 *
 * Part of the public surface, because a test of stage-dependent UI lives in the product, not here —
 * and hand-rolling the tag per product is how one of them ends up injecting a channel nothing reads.
 */

/**
 * Writes one injected `<meta>` tag, as the entrypoint would have.
 */
export function injectMeta(metaName: string, content: string) {
  const meta = document.createElement("meta");
  meta.setAttribute("name", metaName);
  meta.setAttribute("content", content);
  document.head.appendChild(meta);
}

/**
 * Removes what {@link injectMeta} wrote, and nothing else — a test's `afterEach` must not clear a
 * head that the test setup also writes to.
 */
export function removeInjectedMeta(metaName: string) {
  document.head.querySelectorAll(`meta[name='${metaName}']`).forEach((meta) => meta.remove());
}
