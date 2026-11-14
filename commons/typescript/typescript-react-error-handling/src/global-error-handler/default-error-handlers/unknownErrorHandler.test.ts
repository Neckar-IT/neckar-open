/**
 * Imports
 */

import { UNSAFE_ErrorResponseImpl as ErrorResponseImpl } from "react-router";
import { describe, expect, it } from "vitest";
import { MaxThrownValueLength } from "../formatErrorBody";
import { describeThrownValue } from "./unknownErrorHandler";

/**
 * Tests
 */

describe("describeThrownValue", () => {
  it("takes message and stack trace of an Error", () => {
    const error = new Error("boom");

    expect(describeThrownValue(error)).toEqual({ errorMessage: "boom", stackTrace: error.stack });
  });

  it("takes a rejected string as the message", () => {
    expect(describeThrownValue("plain string rejection")).toEqual({ errorMessage: "plain string rejection" });
  });

  it("names the cause of a React Router error response instead of serialising it", () => {
    const cause = new Error('No route matches URL "/missing"');

    // React Router moves an Error passed as data into the response's private `error` field.
    expect(describeThrownValue(new ErrorResponseImpl(404, "Not Found", cause, true))).toEqual({
      errorMessage: 'No route matches URL "/missing"',
      stackTrace: cause.stack,
    });
  });

  it("keeps the detail a loader throws with a React Router error response", () => {
    expect(describeThrownValue(new ErrorResponseImpl(404, "Not Found", "Book 42 does not exist"))).toEqual({
      errorMessage: "404 Not Found: Book 42 does not exist",
    });
  });

  it("falls back to status and status text when the response carries neither cause nor data", () => {
    expect(describeThrownValue(new ErrorResponseImpl(404, "Not Found", null))).toEqual({ errorMessage: "404 Not Found" });
  });

  it("caps a long thrown value and a long response detail", () => {
    const tooLong = "x".repeat(MaxThrownValueLength + 1);
    const marker = `(truncated, ${MaxThrownValueLength + 1} characters total)`;

    expect(describeThrownValue(tooLong).errorMessage).toContain(marker);
    expect(describeThrownValue(new ErrorResponseImpl(500, "Internal Server Error", tooLong)).errorMessage).toContain(marker);
  });

  it("describes symbols and bigints, which JSON cannot serialise", () => {
    expect(describeThrownValue(Symbol("reason"))).toEqual({ errorMessage: "Symbol(reason)" });
    expect(describeThrownValue(10n)).toEqual({ errorMessage: "10" });
  });

  it("spells out null and the empty string, which formatErrorBody declines", () => {
    expect(describeThrownValue(null)).toEqual({ errorMessage: "null" });
    expect(describeThrownValue("")).toEqual({ errorMessage: '""' });
  });

  it("serialises a plain object and names undefined", () => {
    expect(describeThrownValue({ code: 42 }).errorMessage).toContain('"code": 42');
    expect(describeThrownValue(undefined)).toEqual({ errorMessage: "undefined" });
  });
});
