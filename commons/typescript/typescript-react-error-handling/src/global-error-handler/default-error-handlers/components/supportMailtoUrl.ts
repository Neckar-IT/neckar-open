/**
 * Imports
 */

import { buildMailtoUrl } from "./mailtoUrl";

/**
 * Constants
 */

/**
 * Longest `mailto:` URL the support link produces. Some mail clients cut or reject longer ones;
 * 2000 characters is the common safe bound.
 */
export const MaxMailtoUrlLength = 2000;

/**
 * Type Declarations
 */

export interface SupportMail {
  contactEmail: string;
  subject: string;
  /** Text above the error information. */
  bodyIntro: string;
  errorInfo: string;
  /** Closes error information that had to be truncated. */
  truncationHint: string;
}

/**
 * Support Mail
 */

/**
 * Builds the `mailto:` URL of the support mail, at most [MaxMailtoUrlLength] characters long. Error
 * information that does not fit is truncated after its last fitting character and closed with the hint.
 */
export function buildSupportMailtoUrl(mail: SupportMail): string {
  // Measured on the encoded URL, where a single umlaut already takes six characters.
  const urlWithErrorInfo = (errorInfo: string) => buildMailtoUrl(mail.contactEmail, mail.subject, `${mail.bodyIntro}\n\n${errorInfo}`);

  // Code points, so a cut never splits a surrogate pair; encodeURIComponent throws on a lone surrogate.
  const codePoints = Array.from(mail.errorInfo, (codePoint) => (isLoneSurrogate(codePoint) ? "\uFFFD" : codePoint));

  const completeUrl = urlWithErrorInfo(codePoints.join(""));
  if (completeUrl.length <= MaxMailtoUrlLength) {
    return completeUrl;
  }

  const urlWithPrefix = (codePointCount: number) =>
    urlWithErrorInfo(`${codePoints.slice(0, codePointCount).join("")}\n\n${mail.truncationHint}`);

  if (urlWithPrefix(0).length > MaxMailtoUrlLength) {
    throw new Error(`Subject, intro and truncation hint of the support mail alone exceed ${MaxMailtoUrlLength} characters`);
  }

  // Invariant: a prefix of `fitting` code points fits, one of `tooMany` does not. Every code point
  // encodes to at least one character, so no prefix longer than the limit can fit.
  let fitting = 0;
  let tooMany = Math.min(codePoints.length, MaxMailtoUrlLength);
  while (tooMany - fitting > 1) {
    const candidate = Math.floor((fitting + tooMany) / 2);
    if (urlWithPrefix(candidate).length <= MaxMailtoUrlLength) {
      fitting = candidate;
    } else {
      tooMany = candidate;
    }
  }
  return urlWithPrefix(fitting);
}

/**
 * `Array.from` yields a surrogate pair as one element, so a single surrogate unit is always unpaired.
 */
function isLoneSurrogate(codePoint: string): boolean {
  const codeUnit = codePoint.charCodeAt(0);
  return codePoint.length === 1 && codeUnit >= 0xd800 && codeUnit <= 0xdfff;
}
