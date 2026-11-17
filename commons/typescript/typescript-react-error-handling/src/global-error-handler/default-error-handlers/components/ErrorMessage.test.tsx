/**
 * Imports
 */

import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ErrorMessage } from "./ErrorMessage";

/**
 * Helpers
 */

function renderedText(props: Parameters<typeof ErrorMessage>[0]): string | null {
  const { container } = render(<ErrorMessage {...props} />);
  return container.querySelector("pre")?.textContent ?? null;
}

/**
 * Tests
 */

describe("ErrorMessage", () => {
  it("joins the exception type and the message of a 4xx answer", () => {
    expect(renderedText({ exceptionType: "it.neckar.rest.InvalidQueryParameterException", errorMessage: "Invalid value [abc]" })).toBe(
      "it.neckar.rest.InvalidQueryParameterException: Invalid value [abc]",
    );
  });

  it("shows the exception type alone for a 5xx answer without a message", () => {
    expect(renderedText({ exceptionType: "java.lang.IllegalStateException", errorMessage: null })).toBe("java.lang.IllegalStateException");
  });

  it("shows the message alone without a prefix", () => {
    expect(renderedText({ errorMessage: "boom" })).toBe("boom");
  });

  it("names the missing message when neither prefix nor message is given", () => {
    expect(renderedText({ errorMessage: "" })).toBe("No error message available.");
  });

  it("names the missing message behind an error code", () => {
    expect(renderedText({ errorCode: "DB-100", errorMessage: null })).toBe("DB-100: No error message available.");
  });
});
