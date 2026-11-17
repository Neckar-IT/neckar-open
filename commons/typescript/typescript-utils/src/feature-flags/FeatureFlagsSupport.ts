import type { FeatureFlag } from "./FeatureFlag";
import { FeatureFlags } from "./FeatureFlags";

/**
 * Support for global feature flags.
 * Manages the global state of active feature flags.
 */
export class FeatureFlagsSupport {
  /**
   * Global variable containing the feature flags
   */
  private static _flags: FeatureFlags = FeatureFlags.empty;

  /**
   * Adds a feature flag to the active flags
   */
  static add(additionalFlag: FeatureFlag): void {
    this._flags = this._flags.withAdditional(additionalFlag);
  }

  /**
   * Removes a feature flag from the active flags
   */
  static remove(flag: FeatureFlag): void {
    this._flags = this._flags.withRemovedFlag(flag);
  }

  /**
   * Sets the state of the given feature flag
   */
  static set(flag: FeatureFlag, flagState: boolean): void {
    if (flagState) {
      this.add(flag);
    } else {
      this.remove(flag);
    }
  }

  /**
   * Returns true if the provided feature flag is active
   */
  static active(featureFlag: FeatureFlag): boolean {
    return this._flags.contains(featureFlag);
  }

  /**
   * Returns all currently active flags
   */
  static get flags(): FeatureFlags {
    return this._flags;
  }

  /**
   * Sets all active flags
   */
  static set flags(value: FeatureFlags) {
    this._flags = value;
  }
}
