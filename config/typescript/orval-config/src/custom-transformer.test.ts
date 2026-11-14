/* eslint-disable quotes */

/**
 * Imports
 */

import type { GeneratorVerbOptions, OpenApiParameterObject } from "@orval/core";
import { describe, expect, it, vi } from "vitest";
import { customTransformer } from "./custom-transformer";

/**
 * Helpers
 */

function verbOptions(parameters: OpenApiParameterObject[]): GeneratorVerbOptions {
  return {
    doc: "",
    operationId: "getAuthorTableEntries",
    originalOperation: {
      parameters,
    },
    response: {},
  } as unknown as GeneratorVerbOptions;
}

/**
 * Tests
 */

describe("custom-transformer", () => {
  it("should document a parameter with an inline schema", () => {
    const data = customTransformer(
      verbOptions([
        {
          name: "authorId",
          in: "path",
          required: true,
          schema: { type: "string", "x-source-kotlin-type": "kotlin.String" },
        } as unknown as OpenApiParameterObject,
      ]),
    );

    expect(data.doc).toBe('/**\n * @param {"string"} authorId (kotlin.String)\n */\n');
  });

  it("should document a parameter that points at a named schema by that schema name", () => {
    const data = customTransformer(
      verbOptions([
        {
          name: "archiveFilter",
          in: "query",
          required: false,
          "x-source-kotlin-type": "it.neckar.rest.archive.ArchiveFilter",
          schema: { $ref: "#/components/schemas/ArchiveFilter" },
        } as unknown as OpenApiParameterObject,
      ]),
    );

    expect(data.doc).toBe(
      '/**\n * @param {Object} query\n * @param {"ArchiveFilter"} query.archiveFilter (it.neckar.rest.archive.ArchiveFilter)\n */\n',
    );
  });

  it("should omit the Kotlin type when the specification does not state one", () => {
    const data = customTransformer(
      verbOptions([
        {
          name: "sortBy",
          in: "query",
          required: false,
          schema: { $ref: "#/components/schemas/AuthorTableEntriesResponse.SortCriteria" },
        } as unknown as OpenApiParameterObject,
      ]),
    );

    expect(data.doc).toBe(
      '/**\n * @param {Object} query\n * @param {"AuthorTableEntriesResponse.SortCriteria"} query.sortBy\n */\n',
    );
  });

  it("should report a parameter whose schema states neither a type nor a reference", () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);

    const data = customTransformer(
      verbOptions([{ name: "pageIndex", in: "query", required: false, schema: {} } as unknown as OpenApiParameterObject]),
    );

    expect(data.doc).toBe("/**\n * @param {Object} query\n * @param query.pageIndex\n */\n");
    expect(warn).toHaveBeenCalledWith(
      'No "type" and no "$ref" for parameter "pageIndex" of operation "getAuthorTableEntries", schema: {}',
    );

    warn.mockRestore();
  });
});
