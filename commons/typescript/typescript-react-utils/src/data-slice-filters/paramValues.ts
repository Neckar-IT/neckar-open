import { splitByWhitespace } from "@neckar-hub/typescript-utils/lang";
import type { ParamCodec } from "./paramCodecs";

/** The tokens of the parameter `freeText`; `undefined` while it is empty or the table has none. */
export function freeTextTokensOf(values: Readonly<Record<string, unknown>>): string[] | undefined {
  const freeText = values["freeText"];
  return splitByWhitespace(typeof freeText === "string" ? freeText : undefined);
}

/** The codec of a parameter; a name outside the definition is a programming error. */
export function requiredCodec(codecOf: ReadonlyMap<string, ParamCodec<unknown>>, name: string): ParamCodec<unknown> {
  const codec = codecOf.get(name);
  if (codec === undefined) {
    throw new Error(`No codec for the parameter "${name}" in the definition`);
  }
  return codec;
}
