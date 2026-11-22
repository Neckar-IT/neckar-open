/**
 * Mirror of the open-vocabulary value class `it.neckar.financial.payment.card.CardNetwork`: the wire type is a plain string, and these
 * are the networks of `CardNetwork.knownNetworks` the dropdown offers. Any other value round-trips unchanged.
 */
export const CardNetworks = {
  Visa: "Visa",
  Mastercard: "Mastercard",
  Amex: "Amex",
} as const;

export type CardNetwork = (typeof CardNetworks)[keyof typeof CardNetworks];

/** `CardNetwork.knownNetworks` in its order; `CardNetwork.cases.json` pins both lists. */
export const knownCardNetworks: readonly CardNetwork[] = Object.values(CardNetworks);

/** Seeds the dropdown: a stored value outside the known set falls back to "none selected". */
export function isKnownCardNetwork(value: string | null | undefined): value is CardNetwork {
  return value != null && (knownCardNetworks as readonly string[]).includes(value);
}
