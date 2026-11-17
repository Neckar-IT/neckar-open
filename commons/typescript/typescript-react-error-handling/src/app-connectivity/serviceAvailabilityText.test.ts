/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import de from "../localization/de.json";
import en from "../localization/en.json";
import type { ServiceAvailability } from "./appConnectivityState";
import { connectivityNotice, serviceAvailabilityText } from "./serviceAvailabilityText";

/**
 * Fixtures
 */

function resolve(bundle: unknown, key: string | undefined): string | undefined {
  if (key === undefined) {
    return undefined;
  }
  return key.split(".").reduce<unknown>((node, segment) => (node as Record<string, unknown>)[segment], bundle) as string;
}

/**
 * Tests
 */

describe("serviceAvailabilityText", () => {
  it.each<[ServiceAvailability, string, string, string | undefined]>([
    [
      { type: "available", state: "degraded" },
      "Einige Funktionen sind gerade eingeschränkt.",
      "Some features are limited right now.",
      undefined,
    ],
    [
      { type: "notReady", state: "starting" },
      "Die Anwendung startet gerade.",
      "The application is starting.",
      "Sobald die Anwendung wieder bereit ist, geht es hier von selbst weiter.",
    ],
    [
      { type: "notReady", state: "migrating" },
      "Gerade laufen Wartungsarbeiten.",
      "Maintenance is in progress.",
      "Sobald die Anwendung wieder bereit ist, geht es hier von selbst weiter.",
    ],
    [
      { type: "notReady", state: "dependency-unavailable" },
      "Die Anwendung ist kurz nicht verfügbar.",
      "The application is briefly unavailable.",
      "Sobald die Anwendung wieder bereit ist, geht es hier von selbst weiter.",
    ],
    [
      { type: "notReady", state: "schema-incompatible" },
      "Die Anwendung ist gerade nicht verfügbar. Wir sind informiert.",
      "The application is unavailable right now. We have been notified.",
      undefined,
    ],
    [
      { type: "notReady", state: "migration-failed" },
      "Die Anwendung ist gerade nicht verfügbar. Wir sind informiert.",
      "The application is unavailable right now. We have been notified.",
      undefined,
    ],
    [
      { type: "notReady", state: "fatal" },
      "Die Anwendung ist gerade nicht verfügbar. Wir sind informiert.",
      "The application is unavailable right now. We have been notified.",
      undefined,
    ],
    [
      { type: "unreachable" },
      "Die Anwendung ist gerade nicht erreichbar.",
      "The application cannot be reached right now.",
      "Sobald die Anwendung wieder bereit ist, geht es hier von selbst weiter.",
    ],
    [
      { type: "noConnection" },
      "Keine Verbindung zur Anwendung.",
      "No connection to the application.",
      "Sobald die Anwendung wieder bereit ist, geht es hier von selbst weiter.",
    ],
  ])("resolves the texts of %j", (availability, germanText, englishText, germanAddition) => {
    const text = serviceAvailabilityText(availability);

    expect(resolve(de, text?.textKey)).toBe(germanText);
    expect(resolve(en, text?.textKey)).toBe(englishText);
    expect(resolve(de, text?.additionKey)).toBe(germanAddition);
    expect(resolve(en, text?.additionKey)).toBe(
      germanAddition === undefined ? undefined : "As soon as the application is ready again, this page continues by itself.",
    );
  });

  it("shows no text while the service is ready", () => {
    expect(serviceAvailabilityText({ type: "available", state: "ready" })).toBeUndefined();
  });
});

describe("connectivityNotice", () => {
  it("names the offline device before every availability", () => {
    expect(connectivityNotice(false, { type: "unreachable" })).toEqual({ textKey: "appConnectivity.deviceOffline", kind: "deviceOffline" });
  });

  it("locks every action unless the service is available, and only hints at a degraded service", () => {
    expect(connectivityNotice(true, { type: "notReady", state: "migrating" })?.kind).toBe("serviceUnavailable");
    expect(connectivityNotice(true, { type: "unreachable" })?.kind).toBe("serviceUnavailable");
    expect(connectivityNotice(true, { type: "available", state: "degraded" })?.kind).toBe("hint");
    expect(connectivityNotice(true, { type: "available", state: "ready" })).toBeUndefined();
    expect(connectivityNotice(true, undefined)).toBeUndefined();
  });
});
