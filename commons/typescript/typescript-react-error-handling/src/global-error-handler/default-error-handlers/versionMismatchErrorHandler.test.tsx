/**
 * Imports
 */

import { ExpectedApplicationErrorWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import { describe, expect, it } from "vitest";
import { makeAxiosError } from "../axiosErrorFixture";
import { versionMismatchErrorHandler } from "./versionMismatchErrorHandler";

/**
 * A wrong prefix sends every real version mismatch to the generic expected-error page, and nothing else fails.
 * The codes copy `RestProtocolVersionProblemException.Cause` and change with it.
 */

/**
 * The fields of a `RestProtocolVersionProblemException` body that the handler reads.
 */
function protocolVersionResponse(errorCode: string) {
  return {
    type: ExpectedApplicationErrorWithTypeType.ExpectedError,
    errorCode,
  };
}

describe("versionMismatchErrorHandler", () => {
  it.each(["PVE-100", "PVE-101"])("renders the reload page for %s", (errorCode) => {
    const handled = versionMismatchErrorHandler(makeAxiosError({ status: 400, responseData: protocolVersionResponse(errorCode) }));

    expect(handled).not.toBeNull();
  });

  it("leaves an expected error with another code to the next handler", () => {
    const handled = versionMismatchErrorHandler(makeAxiosError({ status: 400, responseData: protocolVersionResponse("DB-100") }));

    expect(handled).toBeNull();
  });
});
