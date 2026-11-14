/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { clientEnvironmentOf, type ClientEnvironment } from "./clientEnvironment";

/**
 * Tests
 */

describe("clientEnvironmentOf", () => {
  it.each<[string, ClientEnvironment]>([
    [
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
      { browser: "Chrome", browserVersion: "120.0.0.0", operatingSystem: "Windows" },
    ],
    [
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.2210.91",
      { browser: "Edge", browserVersion: "120.0.2210.91", operatingSystem: "Windows" },
    ],
    [
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 OPR/105.0.4970.34",
      { browser: "Opera", browserVersion: "105.0.4970.34", operatingSystem: "Windows" },
    ],
    [
      "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0",
      { browser: "Firefox", browserVersion: "121.0", operatingSystem: "Linux" },
    ],
    [
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
      { browser: "Safari", browserVersion: "17.1", operatingSystem: "macOS" },
    ],
    [
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:121.0) Gecko/20100101 Firefox/121.0",
      { browser: "Firefox", browserVersion: "121.0", operatingSystem: "macOS" },
    ],
    [
      "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36",
      { browser: "Chrome", browserVersion: "120.0.6099.144", operatingSystem: "Android" },
    ],
    [
      "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1.2 Mobile/15E148 Safari/604.1",
      { browser: "Safari", browserVersion: "17.1.2", operatingSystem: "iOS" },
    ],
    [
      "Mozilla/5.0 (iPad; CPU OS 17_1_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1",
      { browser: "Chrome", browserVersion: "120.0.6099.119", operatingSystem: "iOS" },
    ],
  ])("names browser, version and operating system of %s", (userAgent, expected) => {
    expect(clientEnvironmentOf(userAgent)).toEqual(expected);
  });

  it("reports nothing rather than a guess where the user agent matches no known shape", () => {
    expect(clientEnvironmentOf("SupportBot/1.0")).toEqual({
      browser: undefined,
      browserVersion: undefined,
      operatingSystem: undefined,
    });
  });
});
