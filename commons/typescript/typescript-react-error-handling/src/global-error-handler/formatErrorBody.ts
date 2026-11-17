/**
 * Request-body payloads above this length are truncated — a request body is
 * context, not the primary diagnosis, so a short prefix is enough.
 */
export const MaxRequestBodyLength = 2000;

/**
 * Response-body payloads above this length are truncated. The response body
 * carries the failure detail, so the cap is generous, but a 500 answered with a
 * full HTML page must not flood the DOM or the clipboard.
 */
export const MaxResponseBodyLength = 20000;

/** Cap for a thrown value that is no Error; it is the diagnosis, capped like [MaxResponseBodyLength]. */
export const MaxThrownValueLength = MaxResponseBodyLength;

/**
 * Formats a payload, or a thrown value that is no Error, for the error page and the error
 * information; `undefined` when empty. Beyond `maxLength` it keeps the start and appends a marker.
 */
export function formatErrorBody(data: unknown, maxLength?: number): string | undefined {
  if (data === undefined || data === null || data === "") {
    return undefined;
  }

  let text: string;
  if (typeof data === "string") {
    text = data;
  } else if (typeof data === "number" || typeof data === "boolean") {
    text = String(data);
  } else if (data instanceof Blob) {
    // An unread Blob serialises as `{}`; size and type are what is known without awaiting it.
    text = `[Blob, ${data.size} bytes${data.type ? `, ${data.type}` : ""}]`;
  } else {
    try {
      text = JSON.stringify(data, null, 2) ?? "[unserializable]";
    } catch {
      text = "[unserializable]";
    }
  }

  if (maxLength !== undefined && text.length > maxLength) {
    // Keeps a surrogate pair whole, so page and clipboard show no broken character.
    const lastKept = text.charCodeAt(maxLength - 1);
    const cut = lastKept >= 0xd800 && lastKept <= 0xdbff ? maxLength - 1 : maxLength;
    return `${text.slice(0, cut)}\n… (truncated, ${text.length} characters total)`;
  }

  return text;
}
