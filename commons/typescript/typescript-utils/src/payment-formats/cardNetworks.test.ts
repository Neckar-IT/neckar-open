import { describe, expect, it } from "vitest";
import { isKnownCardNetwork, knownCardNetworks } from "./cardNetworks";
import { loadPaymentFormatCases } from "./paymentFormatsTestSupport";

interface CardNetworkCorpus {
  purpose: string;
  known: string[];
}

const corpus = loadPaymentFormatCases<CardNetworkCorpus>("commons/payment/src/jvmTest/resources/CardNetwork.cases.json");

describe("knownCardNetworks", () => {
  it("lists the networks of CardNetwork.knownNetworks", () => {
    expect(knownCardNetworks).toEqual(corpus.known);
  });

  it("treats a network outside the known set as unknown", () => {
    expect([isKnownCardNetwork("Visa"), isKnownCardNetwork("girocard"), isKnownCardNetwork(null)]).toEqual([true, false, false]);
  });
});
