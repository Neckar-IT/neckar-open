import { type Key, validateKey } from "./Key";

/**
 * Describes a feature flag.
 *
 * Feature flags enable dynamic toggling of functionality at runtime without deployment.
 * They are identified solely by their key; the description is optional and used for documentation only.
 *
 * Note: Available feature flags are defined on the backend and fetched via the API.
 * Use `fetchAvailableFeatureFlags()` from FeatureFlagsApi to get the list of available flags.
 */
export class FeatureFlag {
  /**
   * The key of the feature flag.
   * Feature flags are identified *only* by their key.
   */
  readonly key: Key;

  /**
   * The optional description.
   * Used for documentation purposes only; not transferred or stored.
   */
  readonly description?: string;

  constructor(key: Key, description?: string) {
    validateKey(key);
    this.key = key;
    this.description = description;
  }

  equals(other: FeatureFlag): boolean {
    return this.key === other.key;
  }

  /**
   * Creates a FeatureFlag for the given key.
   */
  static forKey(key: Key): FeatureFlag {
    return new FeatureFlag(key);
  }
}
