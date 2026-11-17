/**
 * Combine replace the dynamic path params with their actual values
 */

export function buildUrl(link: {
  to: PropertyKey;
  params?: Record<string, string>;
  searchParams?: URLSearchParams | Record<string, string | undefined>;
}) {
  let url = link.to as string;

  if (link.params !== undefined) {
    for (const [key, value] of Object.entries(link.params)) {
      // replaceAll: a pattern can repeat the same param (e.g. /u/:id/m/:id);
      // String.replace would only substitute the first occurrence and leave a stale ":id".
      url = url.replaceAll(`:${key}`, value);
    }
  }

  if ("searchParams" in link && link.searchParams !== undefined) {
    const params =
      link.searchParams instanceof URLSearchParams
        ? link.searchParams
        : new URLSearchParams(Object.entries(link.searchParams).filter((entry): entry is [string, string] => entry[1] !== undefined));
    const queryString = params.toString();
    if (queryString) {
      url += `?${queryString}`;
    }
  }

  return url;
}

/**
 * Extract the names of the dynamic path parameters
 */

export function extractParamNames(pattern: string): string[] {
  return Array.from(pattern.matchAll(/:([A-Za-z0-9_]+)/g)).map((match) => {
    const paramName = match[1];
    if (paramName === undefined) {
      throw new Error(`Path parameter name missing in match [${match[0]}] of pattern [${pattern}]`);
    }
    return paramName;
  });
}
