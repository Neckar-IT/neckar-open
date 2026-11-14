import { QueryStatus, type QueryResultSuccess } from "@neckar-hub/typescript-react-query";
import type { RestResponse } from "@neckar-hub/typescript-utils/rest";
import { describe, expect, it } from "vitest";
import { BadResponseError } from "./BadResponseError";
import { requireGoodResponse } from "./requireGoodResponse";

function successResult(response: RestResponse<string>, entityLabel?: string): QueryResultSuccess<RestResponse<string>> {
  return {
    status: QueryStatus.Success,
    response,
    meta: entityLabel === undefined ? undefined : { entityLabel },
    queryKey: ["test"],
  };
}

describe("requireGoodResponse", () => {
  it("does not throw for a good response", () => {
    expect(() => requireGoodResponse(successResult({ type: "good", data: "payload" }))).not.toThrow();
  });

  it("throws a BadResponseError carrying badType, message and entityLabel for a bad/ response", () => {
    const result = successResult({ type: "bad/NotFound" }, "Book");

    let thrown: unknown;
    try {
      requireGoodResponse(result, "custom message");
    } catch (error) {
      thrown = error;
    }

    expect(thrown).toBeInstanceOf(BadResponseError);
    const badResponseError = thrown as BadResponseError;
    expect(badResponseError.badType).toBe("bad/NotFound");
    expect(badResponseError.message).toBe("custom message");
    expect(badResponseError.entityLabel).toBe("Book");
  });

  it("throws the contract-explaining error for a discriminator violating the RestResponse contract", () => {
    const result = successResult({ type: "bad" } as unknown as RestResponse<string>);

    expect(() => requireGoodResponse(result)).toThrow(/RestResponse contract/);
    expect(() => requireGoodResponse(result)).toThrow(/@SerialName/);
  });
});
