/**
 * The lookup key of the label of a capability: `patterns:finalize-order` reads `capabilities.patterns.finalize-order`.
 * Each product ships the labels of its own capabilities under that namespace.
 */
export function capabilityTranslationKey(capability: string): string {
  return `capabilities.${capability.split(":").join(".")}`;
}
