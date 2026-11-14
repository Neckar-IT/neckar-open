import type { ServiceAvailability } from "./appConnectivityState";

/**
 * Text keys of one availability: the banner and page title, and the addition `ServiceUnavailablePage` shows below it.
 */
export interface ServiceAvailabilityText {
  textKey: string;
  additionKey: string | undefined;
}

const ContinuesByItself = "serviceAvailability.continuesByItself";

/**
 * The text of an availability, or undefined for `ready`, which shows no banner.
 */
export function serviceAvailabilityText(availability: ServiceAvailability): ServiceAvailabilityText | undefined {
  switch (availability.type) {
    case "available":
      return availability.state === "degraded" ? { textKey: "serviceAvailability.degraded", additionKey: undefined } : undefined;
    case "notReady":
      switch (availability.state) {
        case "starting":
          return { textKey: "serviceAvailability.starting", additionKey: ContinuesByItself };
        case "migrating":
          return { textKey: "serviceAvailability.migrating", additionKey: ContinuesByItself };
        case "dependency-unavailable":
          return { textKey: "serviceAvailability.dependencyUnavailable", additionKey: ContinuesByItself };
        case "schema-incompatible":
        case "migration-failed":
        case "fatal":
          return { textKey: "serviceAvailability.failed", additionKey: undefined };
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(availability)}`);
      }
    case "unreachable":
      return { textKey: "serviceAvailability.unreachable", additionKey: ContinuesByItself };
    case "noConnection":
      return { textKey: "serviceAvailability.noConnection", additionKey: ContinuesByItself };
    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(availability)}`);
  }
}

/**
 * What the banner shows and every action names as its lock reason. `hint` (a degraded service) locks no action.
 */
export interface ConnectivityNotice {
  textKey: string;
  kind: "deviceOffline" | "serviceUnavailable" | "hint";
}

/**
 * The notice for the device and the polled availability, or undefined while both are fine.
 */
export function connectivityNotice(isDeviceOnline: boolean, availability: ServiceAvailability | undefined): ConnectivityNotice | undefined {
  if (!isDeviceOnline) {
    return { textKey: "appConnectivity.deviceOffline", kind: "deviceOffline" };
  }
  const text = availability === undefined ? undefined : serviceAvailabilityText(availability);
  if (text === undefined) {
    return undefined;
  }
  return { textKey: text.textKey, kind: availability?.type === "available" ? "hint" : "serviceUnavailable" };
}
