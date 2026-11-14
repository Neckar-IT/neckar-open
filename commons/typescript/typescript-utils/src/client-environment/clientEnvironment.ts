/**
 * What the browser says about itself, with absence kept as absence.
 *
 * A user agent none of the patterns match leaves every value `undefined`: a support request is
 * served better by a missing value than by a guessed browser name.
 */
export interface ClientEnvironment {
  browser: Browser | undefined;
  browserVersion: string | undefined;
  operatingSystem: OperatingSystem | undefined;
}

interface UserAgentPattern {
  name: string;
  pattern: RegExp;
}

/**
 * The first match wins, so the more specific shapes stand first: Edge and Opera carry `Chrome` in
 * their user agent, Chrome carries `Safari`. Each pattern captures the version as its first group.
 */
const BrowserPatterns = [
  { name: "Edge", pattern: /Edg(?:e|A|iOS)?\/([\d.]+)/ },
  { name: "Opera", pattern: /OPR\/([\d.]+)/ },
  { name: "Firefox", pattern: /(?:Firefox|FxiOS)\/([\d.]+)/ },
  { name: "Chrome", pattern: /(?:Chrome|CriOS)\/([\d.]+)/ },
  { name: "Safari", pattern: /Version\/([\d.]+) (?:Mobile\/\S+ )?Safari\// },
] as const satisfies readonly UserAgentPattern[];

export type Browser = (typeof BrowserPatterns)[number]["name"];

/**
 * The first match wins here too: an iPhone reports `like Mac OS X`, an Android device reports
 * `Linux`.
 */
const OperatingSystemPatterns = [
  { name: "Windows", pattern: /Windows NT/ },
  { name: "Android", pattern: /Android/ },
  { name: "iOS", pattern: /iPhone|iPad|iPod/ },
  { name: "macOS", pattern: /Mac OS X/ },
  { name: "Linux", pattern: /Linux/ },
] as const satisfies readonly UserAgentPattern[];

export type OperatingSystem = (typeof OperatingSystemPatterns)[number]["name"];

/**
 * Reads the browser the application runs in.
 */
export function readClientEnvironment(): ClientEnvironment {
  return clientEnvironmentOf(globalThis.navigator.userAgent);
}

/**
 * Takes the user agent as a parameter, so a test states the browser it describes instead of the one
 * it runs in.
 */
export function clientEnvironmentOf(userAgent: string): ClientEnvironment {
  const browser = matchBrowser(userAgent);

  return {
    browser: browser?.name,
    browserVersion: browser?.version,
    operatingSystem: OperatingSystemPatterns.find((candidate) => candidate.pattern.test(userAgent))?.name,
  };
}

function matchBrowser(userAgent: string): { name: Browser; version: string } | undefined {
  for (const candidate of BrowserPatterns) {
    const version = candidate.pattern.exec(userAgent)?.[1];

    if (version !== undefined) {
      return { name: candidate.name, version };
    }
  }

  return undefined;
}
