/**
 * Builds a `mailto:` URL. Without a recipient the mail client asks for one.
 */
export function buildMailtoUrl(recipient: string | null, subject: string, body: string): string {
  return `mailto:${recipient ?? ""}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
}
