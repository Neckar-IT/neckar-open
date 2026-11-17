import { PlatformHeader } from "../platform-headers";
import { FeatureFlag } from "./FeatureFlag";

/**
 * Represents a set of feature flags that are active
 */
export class FeatureFlags {
  readonly flags: Set<FeatureFlag>;

  constructor(flags: Iterable<FeatureFlag>) {
    this.flags = new Set(flags);
  }

  static readonly HeaderKey = PlatformHeader.FeatureFlags;
  static readonly QueryParamName = "featureFlags";
  static readonly empty = new FeatureFlags([]);

  contains(featureFlag: FeatureFlag): boolean {
    for (const flag of this.flags) {
      if (flag.equals(featureFlag)) {
        return true;
      }
    }
    return false;
  }

  toString(): string {
    return `[${Array.from(this.flags)
      .map((f) => f.key)
      .join(", ")}]`;
  }

  encodeToString(): string {
    return Array.from(this.flags)
      .map((f) => f.key)
      .join(",");
  }

  withAdditional(additionalFlag: FeatureFlag): FeatureFlags {
    return new FeatureFlags([...this.flags, additionalFlag]);
  }

  withRemovedFlag(flagToBeRemoved: FeatureFlag): FeatureFlags {
    return new FeatureFlags(Array.from(this.flags).filter((flag) => !flag.equals(flagToBeRemoved)));
  }

  isEmpty(): boolean {
    return this.flags.size === 0;
  }

  static decodeFromString(asString: string | null): FeatureFlags {
    if (!asString) return FeatureFlags.empty;
    const keys = asString
      .split(",")
      .map((k) => k.trim())
      .filter((k) => k.length > 0);
    const flags = keys.map((k) => FeatureFlag.forKey(k));
    return new FeatureFlags(flags);
  }
}
