/**
 * Imports
 */

import type { ClientGeneratorsBuilder, GeneratorClients, GeneratorOptions, GeneratorVerbOptions, GetterProps } from "@orval/core";
import { describe, expect, it } from "vitest";
import { urlFunctionsClient } from "./url-functions-client";

/**
 * Helpers
 */

const baseImplementation = "export const getQuotePdfForBookOrder = (bookOrderId: string) => httpClient<Blob>({ url: `/api/book-orders/${bookOrderId}/quote-pdf` });\n";

const baseBuilder: ClientGeneratorsBuilder = {
  client: () => ({ implementation: baseImplementation, imports: [] }),
};

const pathParameter: GetterProps = [{ name: "bookOrderId", definition: "bookOrderId: string", implementation: "bookOrderId: string", default: undefined, required: true, type: "param" }];

function verbOptions(overrides: Partial<GeneratorVerbOptions>): GeneratorVerbOptions {
  return {
    operationName: "getQuotePdfForBookOrder",
    verb: "get",
    response: { isBlob: true },
    props: pathParameter,
    params: [{ name: "bookOrderId" }],
    queryParams: undefined,
    ...overrides,
  } as unknown as GeneratorVerbOptions;
}

function generatorOptions(urlEncodeParameters = false): GeneratorOptions {
  return {
    route: "/api/book-orders/${bookOrderId}/quote-pdf",
    context: { output: { urlEncodeParameters } },
  } as unknown as GeneratorOptions;
}

async function generate(options: GeneratorVerbOptions, generator: GeneratorOptions = generatorOptions()): Promise<string> {
  const builder = urlFunctionsClient({ "axios-functions": baseBuilder } as unknown as GeneratorClients);
  const client = await builder.client(options, generator, "axios-functions");
  return client.implementation;
}

/**
 * Tests
 */

describe("url-functions-client", () => {
  it("should append a URL function to a binary operation", async () => {
    const implementation = await generate(verbOptions({}));

    expect(implementation).toBe(
      baseImplementation + "\nexport const getQuotePdfForBookOrderUrl = (bookOrderId: string): string => `/api/book-orders/${bookOrderId}/quote-pdf`;\n",
    );
  });

  it("should encode the path parameter when the output encodes parameters", async () => {
    const implementation = await generate(verbOptions({}), generatorOptions(true));

    expect(implementation).toContain("`/api/book-orders/${encodeURIComponent(String(bookOrderId))}/quote-pdf`");
  });

  it("should keep a reserved path parameter unencoded when the output encodes parameters", async () => {
    const implementation = await generate(verbOptions({ params: [{ name: "bookOrderId", allowReserved: true }] as GeneratorVerbOptions["params"] }), generatorOptions(true));

    expect(implementation).toContain("export const getQuotePdfForBookOrderUrl = (bookOrderId: string): string => `/api/book-orders/${bookOrderId}/quote-pdf`;");
  });

  it("should take named path parameters as one object", async () => {
    const namedPathParameters: GetterProps = [{ name: "pathParams", definition: "{ bookOrderId }: GetQuotePdfForBookOrderPathParameters", implementation: "{ bookOrderId }: GetQuotePdfForBookOrderPathParameters", default: undefined, required: true, type: "namedPathParams", schema: undefined, destructured: "{ bookOrderId }" }] as unknown as GetterProps;
    const implementation = await generate(verbOptions({ props: namedPathParameters }));

    expect(implementation).toContain("export const getQuotePdfForBookOrderUrl = ({ bookOrderId }: GetQuotePdfForBookOrderPathParameters): string => ");
  });

  it("should pass a JSON operation through unchanged", async () => {
    const implementation = await generate(verbOptions({ response: { isBlob: false } as GeneratorVerbOptions["response"] }));

    expect(implementation).toBe(baseImplementation);
  });

  it("should pass a binary POST operation through unchanged", async () => {
    const implementation = await generate(verbOptions({ verb: "post" }));

    expect(implementation).toBe(baseImplementation);
  });

  it("should reject a binary operation with query parameters", async () => {
    const options = verbOptions({ queryParams: {} as GeneratorVerbOptions["queryParams"] });

    await expect(generate(options)).rejects.toThrow('Operation "getQuotePdfForBookOrder" has a binary response and query parameters');
  });
});
