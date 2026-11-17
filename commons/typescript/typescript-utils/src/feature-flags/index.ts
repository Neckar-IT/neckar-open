// Core classes
export { FeatureFlag } from "./FeatureFlag";
export { FeatureFlags } from "./FeatureFlags";
export { FeatureFlagsSupport } from "./FeatureFlagsSupport";
export { validateKey, type Key } from "./Key";

// API functions
export { fetchAvailableFeatureFlags, type AvailableFeatureFlag, type AvailableFeatureFlagsResponse } from "./FeatureFlagsApi";

// Extension functions (URL and LocalStorage handling)
export {
  getUrlParameter,
  getUrlWithoutFeatureFlags,
  initializeFeatureFlagsFromUrlOrLocalStorage,
  loadFeatureFlagsFromLocalStorage,
  loadFeatureFlagsFromUrlOrLocalStorage,
  loadFlagsFromUrl,
  parseFeatureFlagsFromUrl,
  writeCurrentFlagsToLocalStorage,
  writeCurrentFlagsToUrl,
  writeFeatureFlagsToLocalStorage,
  writeFeatureFlagsToUrl,
} from "./FeatureFlagsExt";
