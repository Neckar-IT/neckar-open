import { FeatureFlags } from "./FeatureFlags";
import { FeatureFlagsSupport } from "./FeatureFlagsSupport";

const FEATURE_FLAGS_QUERY_PARAM = FeatureFlags.QueryParamName;
const FEATURE_FLAGS_LOCAL_STORAGE_KEY = "featureFlags";

/**
 * Get query parameter from the URL
 */
export function getUrlParameter(name: string): string | null {
  const params = new URLSearchParams(window.location.search);
  return params.get(name);
}

/**
 * Removes any `featureFlags` query parameter from a given URL
 */
export function getUrlWithoutFeatureFlags(url: string): string {
  const parsedUrl = new URL(url);
  parsedUrl.searchParams.delete(FEATURE_FLAGS_QUERY_PARAM);
  return parsedUrl.toString();
}

/**
 * Writes the feature flags to the URL without reloading the page.
 * Uses history.replaceState to avoid page reload.
 */
export function writeFeatureFlagsToUrl(featureFlags: FeatureFlags): void {
  const flagsAsString = featureFlags.encodeToString();
  const baseUrl = getUrlWithoutFeatureFlags(window.location.href);
  const url = flagsAsString ? `${baseUrl}?${FEATURE_FLAGS_QUERY_PARAM}=${encodeURIComponent(flagsAsString)}` : baseUrl;

  window.history.replaceState(null, "", url);
}

/**
 * Writes the current global flags to the URL
 */
export function writeCurrentFlagsToUrl(): void {
  writeFeatureFlagsToUrl(FeatureFlagsSupport.flags);
}

/**
 * Parses feature flags from the URL query parameter.
 *
 * @returns FeatureFlags if URL parameter exists (even if empty), null if parameter doesn't exist
 */
export function parseFeatureFlagsFromUrl(): FeatureFlags | null {
  const urlParameter = getUrlParameter(FEATURE_FLAGS_QUERY_PARAM);
  if (urlParameter === null) {
    // Parameter does not exist in URL
    return null;
  }
  // Parameter exists (may be empty string "" which means explicitly no flags)
  return FeatureFlags.decodeFromString(urlParameter);
}

/**
 * Parses feature flags from URL and sets them as the current global flags
 */
export function loadFlagsFromUrl(): void {
  const parsedFlags = parseFeatureFlagsFromUrl();
  FeatureFlagsSupport.flags = parsedFlags ?? FeatureFlags.empty;
}

/**
 * Saves feature flags to localStorage
 */
export function writeFeatureFlagsToLocalStorage(featureFlags: FeatureFlags): void {
  window.localStorage.setItem(FEATURE_FLAGS_LOCAL_STORAGE_KEY, featureFlags.encodeToString());
}

/**
 * Writes the current global flags to localStorage
 */
export function writeCurrentFlagsToLocalStorage(): void {
  writeFeatureFlagsToLocalStorage(FeatureFlagsSupport.flags);
}

/**
 * Loads feature flags from localStorage.
 *
 * @returns FeatureFlags if localStorage key exists (even if empty), null if key doesn't exist
 */
export function loadFeatureFlagsFromLocalStorage(): FeatureFlags | null {
  const stored = window.localStorage.getItem(FEATURE_FLAGS_LOCAL_STORAGE_KEY);
  if (stored === null) {
    // Key does not exist in localStorage
    return null;
  }
  // Key exists (may be empty string "" which means explicitly no flags)
  return FeatureFlags.decodeFromString(stored);
}

/**
 * Loads feature flags from URL or localStorage (URL takes priority).
 * Returns null if no feature flags are found in either location.
 */
export function loadFeatureFlagsFromUrlOrLocalStorage(): FeatureFlags | null {
  const fromUrl = parseFeatureFlagsFromUrl();
  if (fromUrl !== null) return fromUrl;

  const fromLocalStorage = loadFeatureFlagsFromLocalStorage();
  if (fromLocalStorage !== null) return fromLocalStorage;

  return null;
}

/**
 * Initializes the global feature flags from URL or localStorage.
 * Call this during application startup.
 *
 * If URL parameter exists, it takes priority and also overwrites localStorage.
 * This ensures the URL acts as the single source of truth.
 */
export function initializeFeatureFlagsFromUrlOrLocalStorage(): void {
  const fromUrl = parseFeatureFlagsFromUrl();
  if (fromUrl !== null) {
    // URL parameter exists - use it and sync to localStorage
    FeatureFlagsSupport.flags = fromUrl;
    writeFeatureFlagsToLocalStorage(fromUrl);
    return;
  }

  // No URL parameter - check localStorage
  const fromLocalStorage = loadFeatureFlagsFromLocalStorage();
  if (fromLocalStorage !== null) {
    FeatureFlagsSupport.flags = fromLocalStorage;
  }
}
